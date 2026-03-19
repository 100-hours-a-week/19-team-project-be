package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRequestStatus;
import org.refit.refitbackend.domain.chat.entity.ChatRequestType;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.repository.ChatRequestRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.domain.resume.repository.ResumeRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRequestService {

    private final ChatRequestRepository chatRequestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${ai.base-url:https://dev.re-fit.kr/api/ai}")
    private String aiBaseUrl;

    @Transactional
    public ChatRes.ChatRequestId createRequest(Long requesterId, ChatReq.CreateRequestV2 request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        if (requesterId.equals(request.receiverId())) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        ChatRequestType requestType = parseRequestType(request.requestType());

        if (chatRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                requesterId, request.receiverId(), ChatRequestStatus.PENDING
        )) {
            throw new CustomException(ExceptionType.CHAT_REQUEST_ALREADY_EXISTS);
        }

        validateRequestPayload(requestType, request.resumeId(), request.jobPostUrl());
        validateResumeOwnership(requesterId, request.resumeId());
        validateJobPostCrawlable(requestType, request.jobPostUrl());

        ChatRequest saved = chatRequestRepository.save(ChatRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .resumeId(request.resumeId())
                .requestType(requestType)
                .jobPostUrl(request.jobPostUrl())
                .build());

        notificationService.notifyChatRequestCreated(requester, receiver, saved.getId());

        return ChatRes.ChatRequestId.from(saved);
    }

    public CursorPage<ChatRes.ChatRequestItem> getReceivedRequests(
            Long userId,
            String status,
            Long cursorId,
            int size
    ) {
        ChatRequestStatus requestStatus = parseOptionalRequestStatus(status);
        List<ChatRequest> requests = chatRequestRepository.findReceivedByCursor(
                userId,
                requestStatus,
                cursorId,
                PageRequest.of(0, size + 1)
        );
        return toCursorPage(requests, size);
    }

    public CursorPage<ChatRes.ChatRequestItem> getSentRequests(
            Long userId,
            String status,
            Long cursorId,
            int size
    ) {
        ChatRequestStatus requestStatus = parseOptionalRequestStatus(status);
        List<ChatRequest> requests = chatRequestRepository.findSentByCursor(
                userId,
                requestStatus,
                cursorId,
                PageRequest.of(0, size + 1)
        );
        return toCursorPage(requests, size);
    }

    @Transactional
    public ChatRes.RespondRequestResult respond(Long userId, Long chatRequestId, ChatReq.RespondRequestV2 request) {
        ChatRequest chatRequest = chatRequestRepository.findById(chatRequestId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_NOT_FOUND));

        if (!chatRequest.getReceiver().getId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }

        if (chatRequest.getStatus() != ChatRequestStatus.PENDING) {
            throw new CustomException(ExceptionType.CHAT_ALREADY_RESPONDED);
        }

        ChatRequestStatus targetStatus = parseRespondStatus(request.status());
        Long chatId = null;
        ExpertProfile expertProfile = chatRequest.getReceiver().getExpertProfile();

        if (targetStatus == ChatRequestStatus.ACCEPTED) {
            ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                    .requester(chatRequest.getRequester())
                    .receiver(chatRequest.getReceiver())
                    .chatRequestId(chatRequest.getId())
                    .resumeId(chatRequest.getResumeId())
                    .jobPostUrl(chatRequest.getJobPostUrl())
                    .build());
            chatRequest.accept();
            if (expertProfile != null) {
                expertProfile.applyAcceptedRequest();
            }
            chatId = room.getId();
            notificationService.notifyChatRequestAccepted(
                    chatRequest.getRequester(),
                    chatRequest.getReceiver(),
                    chatRequest.getId(),
                    chatId
            );
        } else {
            chatRequest.reject();
            if (expertProfile != null) {
                expertProfile.applyRejectedRequest();
            }
            notificationService.notifyChatRequestRejected(
                    chatRequest.getRequester(),
                    chatRequest.getReceiver(),
                    chatRequest.getId()
            );
        }

        return new ChatRes.RespondRequestResult(chatRequest.getId(), chatRequest.getStatus().name(), chatId);
    }

    private CursorPage<ChatRes.ChatRequestItem> toCursorPage(List<ChatRequest> requests, int size) {
        boolean hasMore = requests.size() > size;
        if (hasMore) {
            requests = requests.subList(0, size);
        }
        List<ChatRes.ChatRequestItem> items = requests.stream()
                .map(ChatRes.ChatRequestItem::from)
                .toList();
        String nextCursor = requests.isEmpty() ? null : String.valueOf(requests.get(requests.size() - 1).getId());
        return new CursorPage<>(items, nextCursor, hasMore);
    }

    private ChatRequestType parseRequestType(String requestType) {
        if (requestType == null || requestType.isBlank()) {
            throw new CustomException(ExceptionType.CHAT_REQUEST_TYPE_REQUIRED);
        }
        try {
            return ChatRequestType.valueOf(requestType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionType.CHAT_REQUEST_TYPE_INVALID);
        }
    }

    private void validateRequestPayload(ChatRequestType requestType, Long resumeId, String jobPostUrl) {
        if (requestType != ChatRequestType.FEEDBACK) {
            return;
        }
        if (resumeId == null) {
            throw new CustomException(ExceptionType.CHAT_FEEDBACK_CONTEXT_REQUIRED);
        }
        if (jobPostUrl == null || jobPostUrl.isBlank()) {
            throw new CustomException(ExceptionType.CHAT_FEEDBACK_CONTEXT_REQUIRED);
        }
        if (!isValidHttpUrl(jobPostUrl)) {
            throw new CustomException(ExceptionType.CHAT_JOB_POST_URL_INVALID);
        }
    }

    private void validateResumeOwnership(Long requesterId, Long resumeId) {
        if (resumeId == null) {
            return;
        }
        resumeRepository.findByIdAndUserId(resumeId, requesterId)
                .orElseThrow(() -> new CustomException(ExceptionType.RESUME_NOT_FOUND));
    }

    private boolean isValidHttpUrl(String url) {
        String normalized = url.trim().toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private void validateJobPostCrawlable(ChatRequestType requestType, String jobPostUrl) {
        if (requestType != ChatRequestType.FEEDBACK) {
            return;
        }

        String normalizedUrl = jobPostUrl.trim();
        String endpoint = UriComponentsBuilder.fromUriString(aiBaseUrl)
                .path("/repo/job-post")
                .toUriString();

        Map<String, Object> payload = new HashMap<>();
        payload.put("source", detectSource(normalizedUrl));
        payload.put("job_url", normalizedUrl);
        payload.put("job_post_url", normalizedUrl);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (!isValidCrawlResponse(body)) {
                throw new CustomException(ExceptionType.JOB_POST_PARSE_FAILED);
            }
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 422) {
                throw new CustomException(ExceptionType.JOB_POST_PARSE_FAILED);
            }
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private boolean isValidCrawlResponse(Map<String, Object> body) {
        if (body == null || !"OK".equals(String.valueOf(body.get("code")))) {
            return false;
        }
        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map<?, ?> data)) {
            return false;
        }

        // 200이어도 실질적으로 빈 파싱 결과면 실패로 간주한다.
        boolean hasTitle = hasText(data.get("title"));
        boolean hasCompany = hasText(data.get("company"));
        boolean hasRequirements = hasNonEmptyList(data.get("requirements"));
        boolean hasResponsibilities = hasNonEmptyList(data.get("responsibilities"));

        return (hasTitle && hasCompany) || hasRequirements || hasResponsibilities;
    }

    private boolean hasText(Object value) {
        return value != null && !String.valueOf(value).trim().isBlank();
    }

    private boolean hasNonEmptyList(Object value) {
        return value instanceof List<?> list && !list.isEmpty();
    }

    private String detectSource(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("wanted.co.kr")) return "wanted";
        if (lower.contains("saramin.co.kr")) return "saramin";
        if (lower.contains("jobkorea.co.kr")) return "jobkorea";
        if (lower.contains("jumpit.saramin.co.kr")) return "jumpit";
        return "unknown";
    }

    private ChatRequestStatus parseOptionalRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ChatRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionType.CHAT_STATUS_INVALID);
        }
    }

    private ChatRequestStatus parseRespondStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new CustomException(ExceptionType.CHAT_STATUS_REQUIRED);
        }
        try {
            ChatRequestStatus requestStatus = ChatRequestStatus.valueOf(status.trim().toUpperCase());
            if (requestStatus == ChatRequestStatus.ACCEPTED || requestStatus == ChatRequestStatus.REJECTED) {
                return requestStatus;
            }
            throw new CustomException(ExceptionType.CHAT_STATUS_INVALID);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ExceptionType.CHAT_STATUS_INVALID);
        }
    }
}
