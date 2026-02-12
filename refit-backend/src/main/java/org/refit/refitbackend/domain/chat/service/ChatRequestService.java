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
import org.refit.refitbackend.domain.resume.repository.ResumeRepository;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.repository.UserRepository;
import org.refit.refitbackend.global.common.dto.CursorPage;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRequestService {

    private final ChatRequestRepository chatRequestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public ChatRes.ChatRequestId createRequest(Long requesterId, ChatReq.CreateRequestV2 request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new CustomException(ExceptionType.USER_NOT_FOUND));

        if (requesterId.equals(request.receiverId())) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        if (chatRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(
                requesterId, request.receiverId(), ChatRequestStatus.PENDING
        )) {
            throw new CustomException(ExceptionType.CHAT_REQUEST_ALREADY_EXISTS);
        }

        ChatRequestType requestType = parseRequestType(request.requestType());
        validateRequestPayload(requestType, request.resumeId(), request.jobPostUrl());
        validateResumeOwnership(requesterId, request.resumeId());

        ChatRequest saved = chatRequestRepository.save(ChatRequest.builder()
                .requester(requester)
                .receiver(receiver)
                .resumeId(request.resumeId())
                .requestType(requestType)
                .jobPostUrl(request.jobPostUrl())
                .build());

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

        if (targetStatus == ChatRequestStatus.ACCEPTED) {
            ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                    .requester(chatRequest.getRequester())
                    .receiver(chatRequest.getReceiver())
                    .chatRequestId(chatRequest.getId())
                    .resumeId(chatRequest.getResumeId())
                    .jobPostUrl(chatRequest.getJobPostUrl())
                    .build());
            chatRequest.accept();
            chatId = room.getId();
        } else {
            chatRequest.reject();
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
