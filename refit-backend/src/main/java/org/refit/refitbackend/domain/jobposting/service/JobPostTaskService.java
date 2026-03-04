package org.refit.refitbackend.domain.jobposting.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskReq;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskRes;
import org.refit.refitbackend.domain.jobposting.entity.JobPost;
import org.refit.refitbackend.domain.jobposting.entity.JobPostCrawlLog;
import org.refit.refitbackend.domain.jobposting.entity.enums.CrawlStatus;
import org.refit.refitbackend.domain.jobposting.repository.JobPostCrawlLogRepository;
import org.refit.refitbackend.domain.jobposting.repository.JobPostRepository;
import org.refit.refitbackend.domain.task.entity.Task;
import org.refit.refitbackend.domain.task.entity.enums.TaskStatus;
import org.refit.refitbackend.domain.task.entity.enums.TaskTargetType;
import org.refit.refitbackend.domain.task.entity.enums.TaskType;
import org.refit.refitbackend.domain.task.repository.TaskRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostTaskService {

    private final RestTemplate restTemplate;
    private final TaskRepository taskRepository;
    private final JobPostRepository jobPostRepository;
    private final JobPostCrawlLogRepository jobPostCrawlLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.base-url:https://dev.re-fit.kr/api/ai}")
    private String aiBaseUrl;

    @Transactional
    public JobPostTaskRes.TaskResult createTask(Long userId, JobPostTaskReq.CreateTask request) {
        String taskId = "task_job_post_" + UUID.randomUUID().toString().replace("-", "");
        Task task = Task.builder()
                .id(taskId)
                .userId(userId)
                .type(TaskType.JOB_POSTING_CRAWL)
                .status(TaskStatus.PROCESSING)
                .payload(toJson(Map.of(
                        "url", request.url(),
                        "source", request.source()
                )))
                .targetType(TaskTargetType.JOB_POSTING)
                .build();
        taskRepository.save(task);

        try {
            JobPostTaskReq.ParsedData parsedData = requestCrawler(request.url(), request.source());
            Long jobPostId = upsertJobPostAndCompleteTask(task, parsedData);
            return new JobPostTaskRes.TaskResult(task.getId(), task.getStatus().name(), jobPostId);
        } catch (CustomException e) {
            task.markFailed(e.getExceptionType().getCode());
            throw e;
        } catch (Exception e) {
            task.markFailed("INTERNAL_SERVER_ERROR");
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    public JobPostTaskRes.TaskResult getTask(Long userId, String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ExceptionType.TASK_NOT_FOUND));
        if (!task.getUserId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }

        Long jobPostId = null;
        if (task.getTargetType() == TaskTargetType.JOB_POSTING && task.getTargetId() != null) {
            jobPostId = task.getTargetId();
        }
        return new JobPostTaskRes.TaskResult(task.getId(), task.getStatus().name(), jobPostId);
    }

    @Transactional
    public JobPostTaskRes.TaskResult completeTask(Long userId, String taskId, JobPostTaskReq.CompleteTask request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ExceptionType.TASK_NOT_FOUND));
        if (!task.getUserId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }

        Long jobPostId = upsertJobPostAndCompleteTask(task, request.data());
        return new JobPostTaskRes.TaskResult(task.getId(), task.getStatus().name(), jobPostId);
    }

    public JobPostTaskRes.JobPostSimple getJobPost(Long id) {
        JobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionType.INVALID_REQUEST));
        return new JobPostTaskRes.JobPostSimple(
                jobPost.getId(),
                jobPost.getTitle(),
                jobPost.getCompany(),
                jobPost.getEmploymentType()
        );
    }

    @Transactional
    public JobPostTaskRes.CrawlValidation validateCrawl(Long userId, JobPostTaskReq.ValidateCrawl request) {
        String source = detectSource(request.url());
        try {
            JobPostTaskReq.ParsedData parsed = requestCrawler(request.url(), source);
            boolean meaningful = isMeaningfulParsedData(parsed);
            if (!meaningful) {
                return new JobPostTaskRes.CrawlValidation(
                        false, source, 200, ExceptionType.JOB_POST_PARSE_FAILED.getCode(),
                        "크롤링 결과가 비어 있습니다.", parsed.jobPostId(), parsed.title(), parsed.company()
                );
            }
            return new JobPostTaskRes.CrawlValidation(
                    true, source, 200, null, "크롤링 가능",
                    parsed.jobPostId(), parsed.title(), parsed.company()
            );
        } catch (CustomException e) {
            if (e.getExceptionType() == ExceptionType.JOB_POST_PARSE_FAILED) {
                return new JobPostTaskRes.CrawlValidation(
                        false, source, 422, e.getExceptionType().getCode(), e.getExceptionType().getMessage(),
                        null, null, null
                );
            }
            return new JobPostTaskRes.CrawlValidation(
                    false, source, 500, ExceptionType.AI_SERVER_ERROR.getCode(), ExceptionType.AI_SERVER_ERROR.getMessage(),
                    null, null, null
            );
        }
    }

    private String extractSourceFromPayload(String payload) {
        try {
            return objectMapper.readTree(payload).path("source").asText();
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }

    private String extractUrlFromPayload(String payload) {
        try {
            return objectMapper.readTree(payload).path("url").asText();
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }

    private JobPostTaskReq.ParsedData requestCrawler(String url, String source) {
        String resolvedSource = (source == null || source.isBlank()) ? detectSource(url) : source;
        JobPostCrawlLog crawlLog = jobPostCrawlLogRepository.save(JobPostCrawlLog.builder()
                .source(resolvedSource == null || resolvedSource.isBlank() ? "unknown" : resolvedSource)
                .targetUrl(url)
                .status(CrawlStatus.FAILED)
                .startedAt(LocalDateTime.now())
                .build());
        try {
            String endpoint = UriComponentsBuilder
                    .fromUriString(aiBaseUrl)
                    .path("/repo/job-post")
                    .toUriString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("job_url", url);
            payload.put("job_post_url", url);
            if (source != null && !source.isBlank()) {
                payload.put("source", source);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                crawlLog.markFailed(500, "AI body is null");
                jobPostCrawlLogRepository.save(crawlLog);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            Object code = body.get("code");
            if (code == null || !"OK".equals(code.toString())) {
                crawlLog.markFailed(500, "AI code is not OK");
                jobPostCrawlLogRepository.save(crawlLog);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            Object data = body.get("data");
            if (!(data instanceof Map<?, ?> dataMapRaw)) {
                crawlLog.markFailed(500, "AI data is invalid");
                jobPostCrawlLogRepository.save(crawlLog);
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) dataMapRaw;
            JobPostTaskReq.ParsedData parsed = new JobPostTaskReq.ParsedData(
                    extractJobPostId(dataMap),
                    firstNonBlank(
                            nullableText(dataMap.get("title")),
                            nullableText(dataMap.get("job_post_title"))
                    ),
                    firstNonBlank(
                            nullableText(dataMap.get("company")),
                            nullableText(dataMap.get("company_name"))
                    ),
                    firstNonBlank(
                            nullableText(dataMap.get("department")),
                            nullableText(dataMap.get("job_post_position")),
                            nullableText(dataMap.get("position"))
                    ),
                    nullableText(dataMap.get("employment_type")),
                    firstNonBlank(
                            nullableText(dataMap.get("experience_required")),
                            nullableText(dataMap.get("experience_level"))
                    ),
                    firstNonBlank(
                            nullableText(dataMap.get("education_required")),
                            nullableText(dataMap.get("education"))
                    ),
                    firstNonEmptyList(
                            toStringList(dataMap.get("requirements")),
                            toStringList(dataMap.get("qualifications"))
                    ),
                    firstNonEmptyList(
                            toStringList(dataMap.get("preferences")),
                            toStringList(dataMap.get("preferred_qualifications"))
                    ),
                    toStringList(dataMap.get("tech_stack")),
                    toStringList(dataMap.get("responsibilities"))
            );

            if (!isMeaningfulParsedData(parsed)) {
                crawlLog.markFailed(422, "JOB_POST_PARSE_FAILED");
                jobPostCrawlLogRepository.save(crawlLog);
                throw new CustomException(ExceptionType.JOB_POST_PARSE_FAILED);
            }

            crawlLog.markSuccess(response.getStatusCode().value());
            jobPostCrawlLogRepository.save(crawlLog);
            return parsed;
        } catch (HttpStatusCodeException e) {
            crawlLog.markFailed(e.getStatusCode().value(), e.getResponseBodyAsString());
            jobPostCrawlLogRepository.save(crawlLog);
            if (e.getStatusCode().value() == 422) {
                throw new CustomException(ExceptionType.JOB_POST_PARSE_FAILED);
            }
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            if (crawlLog.getFinishedAt() == null) {
                crawlLog.markFailed(500, e.getExceptionType().getCode());
                jobPostCrawlLogRepository.save(crawlLog);
            }
            throw e;
        } catch (Exception e) {
            crawlLog.markFailed(500, e.getMessage());
            jobPostCrawlLogRepository.save(crawlLog);
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private boolean isMeaningfulParsedData(JobPostTaskReq.ParsedData parsed) {
        boolean hasTitleCompany = parsed.title() != null && !parsed.title().isBlank()
                && parsed.company() != null && !parsed.company().isBlank();
        boolean hasRequirements = parsed.requirements() != null && !parsed.requirements().isEmpty();
        boolean hasResponsibilities = parsed.responsibilities() != null && !parsed.responsibilities().isEmpty();
        return hasTitleCompany || hasRequirements || hasResponsibilities;
    }

    private String detectSource(String url) {
        if (url == null) return "unknown";
        String lower = url.toLowerCase();
        if (lower.contains("saramin")) return "saramin";
        if (lower.contains("jobkorea")) return "jobkorea";
        if (lower.contains("wanted")) return "wanted";
        if (lower.contains("jumpit")) return "jumpit";
        return "unknown";
    }

    private Long upsertJobPostAndCompleteTask(Task task, JobPostTaskReq.ParsedData parsed) {
        String source = extractSourceFromPayload(task.getPayload());
        String url = extractUrlFromPayload(task.getPayload());
        String sourceJobId = String.valueOf(parsed.jobPostId());
        String resolvedTitle = parsed.title() == null || parsed.title().isBlank() ? "채용 공고" : parsed.title();
        String resolvedCompany = parsed.company() == null || parsed.company().isBlank() ? "알 수 없음" : parsed.company();

        JobPost jobPost = jobPostRepository.findBySourceAndSourceJobId(source, sourceJobId)
                .orElseGet(() -> JobPost.builder()
                        .source(source)
                        .sourceJobId(sourceJobId)
                        .url(url)
                        .urlHash(sha256(url))
                        .title(resolvedTitle)
                        .company(resolvedCompany)
                        .department(parsed.department())
                        .employmentType(parsed.employmentType())
                        .experienceRequired(parsed.experienceRequired())
                        .educationRequired(parsed.educationRequired())
                        .requirements(toJson(parsed.requirements()))
                        .preferences(toJson(parsed.preferences()))
                        .techStack(toJson(parsed.techStack()))
                        .responsibilities(toJson(parsed.responsibilities()))
                        .descriptionRaw(null)
                        .descriptionClean(null)
                        .postedAt(null)
                        .deadlineAt(null)
                        .isActive(true)
                        .crawledAt(LocalDateTime.now())
                        .build());

        if (jobPost.getId() != null) {
            jobPost.updateFromCrawler(
                    resolvedTitle,
                    resolvedCompany,
                    parsed.department(),
                    parsed.employmentType(),
                    parsed.experienceRequired(),
                    parsed.educationRequired(),
                    toJson(parsed.requirements()),
                    toJson(parsed.preferences()),
                    toJson(parsed.techStack()),
                    toJson(parsed.responsibilities()),
                    null
            );
        }

        JobPost saved = jobPostRepository.save(jobPost);
        task.linkTarget(TaskTargetType.JOB_POSTING, saved.getId());
        task.markCompleted(toJson(Map.of(
                "job_post_id", saved.getId(),
                "source", source,
                "source_job_id", sourceJobId
        )));
        return saved.getId();
    }

    private Long extractJobPostId(Map<String, Object> dataMap) {
        Object id = dataMap.get("job_post_id");
        if (id == null) {
            id = dataMap.get("jobposting_id");
        }
        if (id == null) {
            id = dataMap.get("job_id");
        }
        return toLong(id);
    }

    private Long toLong(Object value) {
        if (value == null) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private String nullableText(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> firstNonEmptyList(List<String>... candidates) {
        if (candidates == null) {
            return List.of();
        }
        for (List<String> candidate : candidates) {
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        return List.of();
    }
}
