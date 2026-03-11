package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRequestType;
import org.refit.refitbackend.domain.chat.entity.ChatReview;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.repository.ChatRequestRepository;
import org.refit.refitbackend.domain.chat.repository.ChatReviewRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.expert.entity.ExpertProfile;
import org.refit.refitbackend.domain.expert.repository.ExpertProfileRepository;
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
public class ChatReviewService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatReviewRepository chatReviewRepository;
    private final ExpertProfileRepository expertProfileRepository;

    @Transactional
    public ChatRes.ChatReviewDetail createReview(Long userId, Long chatId, ChatReq.CreateReviewV3 request) {
        ChatRoom room = getWritableRoom(userId, chatId);
        if (chatReviewRepository.existsByChatRoomId(chatId)) {
            throw new CustomException(ExceptionType.REVIEW_ALREADY_EXISTS);
        }
        validateFeedbackRequestType(room);
        validateReviewPayload(request);

        ChatReview review = chatReviewRepository.save(ChatReview.builder()
                .chatRoom(room)
                .reviewer(room.getRequester())
                .reviewee(room.getReceiver())
                .rating(request.rating())
                .comment(request.comment().trim())
                .build());

        ExpertProfile expertProfile = room.getReceiver().getExpertProfile();
        if (expertProfile != null) {
            expertProfile.applyReview(request.rating());
        }

        return ChatRes.ChatReviewDetail.from(review);
    }

    @Transactional
    public ChatRes.ChatReviewDetail updateReview(Long userId, Long chatId, ChatReq.CreateReviewV3 request) {
        validateReviewPayload(request);
        ChatReview review = getOwnedReview(userId, chatId);

        int oldRating = review.getRating();
        review.update(request.rating(), request.comment().trim());

        ExpertProfile expertProfile = review.getReviewee().getExpertProfile();
        if (expertProfile != null) {
            expertProfile.updateReview(oldRating, request.rating());
        }

        return ChatRes.ChatReviewDetail.from(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long chatId) {
        ChatReview review = getOwnedReview(userId, chatId);

        ExpertProfile expertProfile = review.getReviewee().getExpertProfile();
        if (expertProfile != null) {
            expertProfile.removeReview(review.getRating());
        }

        chatReviewRepository.delete(review);
    }

    public CursorPage<ChatRes.ChatReviewItem> getReviewsByExpertId(Long expertId, Long cursor, int size) {
        if (!expertProfileRepository.existsById(expertId)) {
            throw new CustomException(ExceptionType.EXPERT_NOT_FOUND);
        }

        int pageSize = Math.min(Math.max(size, 1), 50);
        List<ChatReview> reviews = chatReviewRepository.findByRevieweeIdWithReviewerByCursor(
                expertId,
                cursor,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = reviews.size() > pageSize;
        List<ChatReview> pageItems = hasMore ? reviews.subList(0, pageSize) : reviews;
        String nextCursor = hasMore ? String.valueOf(pageItems.get(pageItems.size() - 1).getId()) : null;

        return new CursorPage<>(
                pageItems.stream().map(ChatRes.ChatReviewItem::from).toList(),
                nextCursor,
                hasMore
        );
    }

    private ChatRoom getWritableRoom(Long userId, Long chatId) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (room.getStatus() != ChatRoomStatus.CLOSED) {
            throw new CustomException(ExceptionType.CHAT_NOT_CLOSED);
        }
        if (!room.getRequester().getId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }
        return room;
    }

    private ChatReview getOwnedReview(Long userId, Long chatId) {
        getWritableRoom(userId, chatId);
        return chatReviewRepository.findByChatRoomIdAndReviewerId(chatId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.REVIEW_NOT_FOUND));
    }

    private void validateReviewPayload(ChatReq.CreateReviewV3 request) {
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new CustomException(ExceptionType.REVIEW_RATING_INVALID);
        }
        if (request.comment() == null || request.comment().isBlank()) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        if (request.comment().length() > 300) {
            throw new CustomException(ExceptionType.REVIEW_COMMENT_TOO_LONG);
        }
    }

    private void validateFeedbackRequestType(ChatRoom room) {
        if (room.getChatRequestId() == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        ChatRequest chatRequest = chatRequestRepository.findById(room.getChatRequestId())
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_NOT_FOUND));

        if (chatRequest.getRequestType() != ChatRequestType.FEEDBACK) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
    }
}
