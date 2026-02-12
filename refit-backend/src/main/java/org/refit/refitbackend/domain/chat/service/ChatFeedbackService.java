package org.refit.refitbackend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.dto.ChatReq;
import org.refit.refitbackend.domain.chat.dto.ChatRes;
import org.refit.refitbackend.domain.chat.entity.ChatFeedback;
import org.refit.refitbackend.domain.chat.entity.ChatFeedbackAnswer;
import org.refit.refitbackend.domain.chat.entity.ChatFeedbackQuestion;
import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRequestType;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.repository.ChatFeedbackAnswerRepository;
import org.refit.refitbackend.domain.chat.repository.ChatFeedbackQuestionRepository;
import org.refit.refitbackend.domain.chat.repository.ChatFeedbackRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRequestRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatFeedbackService {

    private static final Pattern COMMA_SPLIT = Pattern.compile("\\s*,\\s*");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRequestRepository chatRequestRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final ChatFeedbackAnswerRepository chatFeedbackAnswerRepository;
    private final ChatFeedbackQuestionRepository chatFeedbackQuestionRepository;

    @Transactional
    public ChatRes.ChatFeedbackId createFeedback(Long userId, Long chatId, ChatReq.CreateFeedbackV2 request) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (room.getStatus() != ChatRoomStatus.CLOSED) {
            throw new CustomException(ExceptionType.CHAT_NOT_CLOSED);
        }
        if (!room.getReceiver().getId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }
        if (chatFeedbackRepository.existsByChatRoomId(chatId)) {
            throw new CustomException(ExceptionType.FEEDBACK_ALREADY_EXISTS);
        }
        validateFeedbackRequestType(room);

        List<ChatReq.FeedbackAnswerV2> answers = request.answers();
        validateAnswerPayload(answers);

        Set<Long> questionIds = new HashSet<>();
        for (ChatReq.FeedbackAnswerV2 answer : answers) {
            if (!questionIds.add(answer.questionId())) {
                throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
            }
        }

        List<ChatFeedbackQuestion> questions = chatFeedbackQuestionRepository.findByIdInAndIsActiveTrue(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
        }

        Map<Long, ChatFeedbackQuestion> questionMap = new HashMap<>();
        for (ChatFeedbackQuestion question : questions) {
            questionMap.put(question.getId(), question);
        }

        for (ChatReq.FeedbackAnswerV2 answer : answers) {
            ChatFeedbackQuestion question = questionMap.get(answer.questionId());
            validateByAnswerType(question.getAnswerType(), answer.answerValue());
        }

        ChatFeedback feedback = chatFeedbackRepository.save(ChatFeedback.builder()
                .chatRoom(room)
                .expert(room.getReceiver())
                .user(room.getRequester())
                .build());

        List<ChatFeedbackAnswer> rows = answers.stream()
                .map(answer -> ChatFeedbackAnswer.builder()
                        .chatFeedback(feedback)
                        .question(questionMap.get(answer.questionId()))
                        .answerValue(answer.answerValue().trim())
                        .build())
                .toList();
        chatFeedbackAnswerRepository.saveAll(rows);

        return new ChatRes.ChatFeedbackId(feedback.getId(), room.getId());
    }

    public ChatRes.ChatFeedbackDetail getFeedback(Long userId, Long chatId) {
        chatRoomRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        ChatFeedback feedback = chatFeedbackRepository.findDetailByChatRoomId(chatId)
                .orElseThrow(() -> new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING));

        List<ChatFeedbackAnswer> answers = chatFeedbackAnswerRepository.findByChatFeedbackIdOrderByQuestion(feedback.getId());
        return ChatRes.ChatFeedbackDetail.from(feedback, answers);
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

    private void validateAnswerPayload(List<ChatReq.FeedbackAnswerV2> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING);
        }
        for (ChatReq.FeedbackAnswerV2 answer : answers) {
            if (answer.questionId() == null || answer.questionId() <= 0) {
                throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
            }
            String value = answer.answerValue();
            if (value == null || value.isBlank() || value.length() > 500) {
                throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
            }
        }
    }

    private void validateByAnswerType(String answerType, String answerValue) {
        if (answerType == null) {
            return;
        }
        String normalizedType = answerType.trim().toUpperCase();
        String trimmed = answerValue.trim();

        if (normalizedType.contains("RADIO") && countSelections(trimmed) != 1) {
            throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
        }
        if ((normalizedType.contains("SELECT3") || normalizedType.contains("MULTI3") || normalizedType.contains("MAX3"))
                && countSelections(trimmed) != 3) {
            throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
        }
        if ((normalizedType.contains("SELECT2") || normalizedType.contains("MULTI2") || normalizedType.contains("MAX2"))
                && countSelections(trimmed) != 2) {
            throw new CustomException(ExceptionType.FEEDBACK_ANSWER_INVALID);
        }
    }

    private int countSelections(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isBlank()) {
            return 0;
        }
        String[] tokens = COMMA_SPLIT.split(normalized);
        int count = 0;
        for (String token : tokens) {
            String item = token.replace("\"", "").trim();
            if (!item.isBlank()) {
                count++;
            }
        }
        return count;
    }
}
