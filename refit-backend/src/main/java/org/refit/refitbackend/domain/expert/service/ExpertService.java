package org.refit.refitbackend.domain.expert.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.expert.dto.ExpertReq;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.refit.refitbackend.domain.expert.dto.ExpertRes;
import org.refit.refitbackend.domain.expert.dto.ExpertSearchRow;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
import org.refit.refitbackend.domain.expert.repository.ExpertRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.UserJob;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserJobRepository;
import org.refit.refitbackend.domain.user.repository.UserSkillRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {
    private final ExpertRepository expertRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final UserJobRepository userJobRepository;
    private final UserSkillRepository userSkillRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.base-url:https://re-fit.kr/api/ai}")
    private String aiBaseUrl;

    public CursorPage<ExpertRes.ExpertListItem> searchExperts(
            String keyword,
            Long jobId,
            Long skillId,
            Long careerLevelId,
            Long cursorId,
            int size
    ) {
        String keywordPattern = toKeywordPattern(keyword);
        if (shouldReturnEmptyOnFirstPage(keywordPattern, cursorId)) {
            return new CursorPage<>(List.of(), null, false);
        }

        List<Long> fetchedIds = fetchExpertIds(keywordPattern, jobId, skillId, careerLevelId, cursorId, size + 1);
        boolean hasMore = fetchedIds.size() > size;
        List<Long> pageIds = hasMore ? fetchedIds.subList(0, size) : fetchedIds;

        List<ExpertSearchRow> expertRows = fetchExpertRows(pageIds);
        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(pageIds);
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(pageIds);

        List<ExpertRes.ExpertListItem> items = toExpertListItems(expertRows, jobsMap, skillsMap);
        String nextCursor = items.isEmpty() ? null : String.valueOf(items.get(items.size() - 1).userId());

        return new CursorPage<>(items, nextCursor, hasMore);
    }

    public ExpertRes.ExpertDetail getExpertDetail(Long userId) {
        User expert = expertRepository.findExpertById(userId)
                .orElseThrow(() -> new CustomException(ExceptionType.EXPERT_NOT_FOUND));

        Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUserIds(List.of(expert.getId()));
        Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUserIds(List.of(expert.getId()));

        ExpertProfile profile = expert.getExpertProfile();

        return ExpertRes.ExpertDetail.from(
                expert,
                jobsMap.getOrDefault(expert.getId(), List.of()),
                skillsMap.getOrDefault(expert.getId(), List.of()),
                profile != null ? profile.getCompanyName() : null,
                profile != null && profile.isVerified(),
                profile != null ? profile.getVerifiedAt() : null,
                profile != null ? profile.getRatingAvg() : 0.0,
                profile != null ? profile.getRatingCount() : 0,
                profile != null ? profile.getLastActiveAt() : null
        );
    }

    public ExpertRes.RecommendationResponse getRecommendations(
            Long userId,
            int topK,
            boolean verified,
            boolean includeEval
    ) {
        String url = UriComponentsBuilder.fromUriString(aiBaseUrl)
                .path("/mentors/recommend/{userId}")
                .queryParam("top_k", topK)
                .queryParam("only_verified", verified)
                .queryParam("include_eval", includeEval)
                .buildAndExpand(userId)
                .toUriString();

        try {
            ResponseEntity<ApiResponse<ExpertRes.RecommendationResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {}
            );

            ApiResponse<ExpertRes.RecommendationResponse> body = response.getBody();
            if (body == null) {
                log.error("AI recommendation response body is null. url={}", url);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            if ("USER_NOT_FOUND".equals(body.code())) {
                throw new CustomException(ExceptionType.USER_NOT_FOUND);
            }
            if (!"OK".equals(body.code()) || body.data() == null) {
                log.error("AI recommendation response invalid. url={}, code={}, hasData={}",
                        url, body.code(), body.data() != null);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            return body.data();
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                String bodyText = e.getResponseBodyAsString();
                try {
                    JsonNode root = objectMapper.readTree(bodyText);
                    String code = root.path("code").asText("");
                    if ("USER_NOT_FOUND".equals(code)) {
                        throw new CustomException(ExceptionType.USER_NOT_FOUND);
                    }
                } catch (Exception ignored) {
                    // ignore parse error and treat as generic AI server failure below
                }
            }
            log.error("AI recommendation call failed. url={}, status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI recommendation unexpected error. url={}", url, e);
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    public ExpertRes.RecommendationResponse getRecommendationsAuto(
            Long authUserId,
            int topK,
            boolean verified,
            boolean includeEval
    ) {
        if (authUserId != null) {
            return getRecommendations(authUserId, topK, verified, includeEval);
        }
        return getPopularRecommendations(topK, verified);
    }

    public ExpertRes.MentorEmbeddingUpdateResponse refreshMentorEmbedding(Long userId) {
        if (!expertProfileRepository.existsById(userId)) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }

        String url = UriComponentsBuilder.fromUriString(aiBaseUrl)
                .path("/mentors/embeddings/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        try {
            ResponseEntity<ApiResponse<ExpertRes.MentorEmbeddingUpdateResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {}
            );

            ApiResponse<ExpertRes.MentorEmbeddingUpdateResponse> body = response.getBody();
            if (body == null) {
                log.error("AI embedding refresh response body is null. url={}", url);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            if ("USER_NOT_FOUND".equals(body.code())) {
                throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
            }
            if (!"OK".equals(body.code()) || body.data() == null) {
                log.error("AI embedding refresh response invalid. url={}, code={}, hasData={}",
                        url, body.code(), body.data() != null);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }

            return body.data();
        } catch (HttpStatusCodeException e) {
            String code = extractAiErrorCode(e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND && "USER_NOT_FOUND".equals(code)) {
                throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
            }
            log.error("AI embedding refresh call failed. url={}, status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI embedding refresh unexpected error. url={}", url, e);
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    public void refreshMentorEmbeddingBestEffort(Long userId) {
        try {
            refreshMentorEmbedding(userId);
        } catch (Exception e) {
            log.warn("Embedding refresh skipped due to AI/server issue. userId={}", userId, e);
        }
    }

    @Transactional
    public void updateEmbedding(ExpertReq.UpdateEmbedding request) {
        if (!expertProfileRepository.existsById(request.userId())) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }
        String embedding = toVectorLiteral(request.embedding());
        int updated = expertProfileRepository.updateEmbedding(request.userId(), embedding);
        if (updated == 0) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }
    }

    private String toVectorLiteral(java.util.List<Double> embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

      private String extractAiErrorCode(String bodyText) {
          if (bodyText == null || bodyText.isBlank()) {
              return "";
          }
          try {
              JsonNode root = objectMapper.readTree(bodyText);
              return root.path("code").asText("");
          } catch (Exception ignored) {
              return "";
          }
      }

      private Map<Long, List<ExpertRes.JobDto>> getJobsByUsers(List<User> users) {
          if (users.isEmpty()) {
              return Map.of();
          }
          List<Long> userIds = users.stream()
                  .map(User::getId)
                  .toList();
          return getJobsByUserIds(userIds);
      }

      private Map<Long, List<ExpertRes.JobDto>> getJobsByUserIds(List<Long> userIds) {
          if (userIds.isEmpty()) {
              return Map.of();
          }
          List<UserJob> userJobs = userJobRepository.findAllByUserIdIn(userIds);

          return userJobs.stream()
                  .collect(Collectors.groupingBy(
                          uj -> uj.getUser().getId(),
                          Collectors.mapping(
                                  uj -> new ExpertRes.JobDto(uj.getJob().getId(), uj.getJob().getName()),
                                  Collectors.toList()
                          )
                  ));
      }

      private Map<Long, List<ExpertRes.SkillDto>> getSkillsByUserIds(List<Long> userIds) {
          if (userIds.isEmpty()) {
              return Map.of();
          }
          List<UserSkill> userSkills = userSkillRepository.findAllByUserIdIn(userIds);

          return userSkills.stream()
                  .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                  .collect(Collectors.groupingBy(
                          us -> us.getUser().getId(),
                          Collectors.mapping(
                                  us -> new ExpertRes.SkillDto(us.getSkill().getId(), us.getSkill().getName()),
                                  Collectors.toList()
                          )
                  ));
      }

      private Map<Long, List<ExpertRes.SkillDto>> getSkillsByUsers(List<User> users) {
          if (users.isEmpty()) {
              return Map.of();
          }
          List<Long> userIds = users.stream()
                  .map(User::getId)
                  .toList();
          return getSkillsByUserIds(userIds);
      }

      private boolean shouldReturnEmptyOnFirstPage(String keywordPattern, Long cursorId) {
          return isFirstPage(cursorId)
                  && keywordPattern != null
                  && !hasAnyKeywordMatch(keywordPattern);
      }

      private boolean isFirstPage(Long cursorId) {
          return cursorId == null;
      }

      private boolean hasAnyKeywordMatch(String keywordPattern) {
          return expertRepository.existsNicknamePrefixMatch(keywordPattern)
                  || expertRepository.existsJobNamePrefixMatch(keywordPattern)
                  || expertRepository.existsSkillNamePrefixMatch(keywordPattern);
      }

      private List<Long> fetchExpertIds(
              String keywordPattern,
              Long jobId,
              Long skillId,
              Long careerLevelId,
              Long cursorId,
              int limit
      ) {
          if (keywordPattern == null) {
              return expertRepository.searchExpertIdsByCursorNoKeyword(
                      jobId, skillId, careerLevelId, cursorId, limit
              );
          }
          return expertRepository.searchExpertIdsByCursorWithKeyword(
                  keywordPattern, jobId, skillId, careerLevelId, cursorId, limit
          );
      }

      private List<ExpertSearchRow> fetchExpertRows(List<Long> userIds) {
          if (userIds.isEmpty()) {
              return List.of();
          }
          return expertRepository.findExpertRowsByUserIds(userIds);
      }

      private List<ExpertRes.ExpertListItem> toExpertListItems(
              List<ExpertSearchRow> expertRows,
              Map<Long, List<ExpertRes.JobDto>> jobsMap,
              Map<Long, List<ExpertRes.SkillDto>> skillsMap
      ) {
          return expertRows.stream()
                  .map(expert -> new ExpertRes.ExpertListItem(
                          expert.userId(),
                          expert.nickname(),
                          expert.profileImageUrl(),
                          expert.introduction(),
                          new ExpertRes.CareerLevelDto(expert.careerLevelId(), expert.careerLevelName()),
                          expert.companyName(),
                          Boolean.TRUE.equals(expert.verified()),
                          expert.ratingAvg() != null ? expert.ratingAvg() : 0.0,
                          expert.ratingCount() != null ? expert.ratingCount() : 0,
                          jobsMap.getOrDefault(expert.userId(), List.of()),
                          skillsMap.getOrDefault(expert.userId(), List.of()),
                          expert.lastActiveAt()
                  ))
                  .toList();
      }

      private String toKeywordPattern(String keyword) {
          if (keyword == null) {
              return null;
          }
          String trimmed = keyword.trim();
          if (trimmed.isEmpty()) {
              return null;
          }
          String escaped = escapeLikeKeyword(trimmed.toLowerCase(Locale.ROOT));
          return escaped + "%";
      }

      private String escapeLikeKeyword(String input) {
          return input
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_");
      }

      private ExpertRes.RecommendationResponse getPopularRecommendations(int topK, boolean verified) {
          List<User> experts = expertRepository.findTopPopularExperts(verified, PageRequest.of(0, topK));
          Map<Long, List<ExpertRes.JobDto>> jobsMap = getJobsByUsers(experts);
          Map<Long, List<ExpertRes.SkillDto>> skillsMap = getSkillsByUsers(experts);

          List<ExpertRes.RecommendationItem> items = experts.stream()
                  .map(user -> {
                      ExpertProfile profile = user.getExpertProfile();
                      List<String> jobs = jobsMap.getOrDefault(user.getId(), List.of())
                              .stream()
                              .map(ExpertRes.JobDto::name)
                              .toList();
                      List<String> skills = skillsMap.getOrDefault(user.getId(), List.of())
                              .stream()
                              .map(ExpertRes.SkillDto::name)
                              .toList();

                      return new ExpertRes.RecommendationItem(
                              user.getId(),
                              user.getNickname(),
                              profile != null ? profile.getCompanyName() : null,
                              user.getProfileImageUrl(),
                              profile != null && profile.isVerified(),
                              profile != null ? profile.getRatingAvg() : 0.0,
                              profile != null ? profile.getRatingCount() : 0,
                              null,
                              skills,
                              jobs,
                              user.getIntroduction(),
                              null,
                              "popular",
                              null,
                              profile != null ? profile.getLastActiveAt() : null
                      );
                  })
                  .toList();

          return new ExpertRes.RecommendationResponse(
                  null,
                  items,
                  items.size(),
                  null
          );
      }
}
