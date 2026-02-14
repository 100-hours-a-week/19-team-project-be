package org.refit.refitbackend.domain.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.chat.entity.ChatFeedback;
import org.refit.refitbackend.domain.chat.entity.ChatFeedbackAnswer;
import org.refit.refitbackend.domain.chat.entity.ChatMessage;
import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.repository.ChatFeedbackAnswerRepository;
import org.refit.refitbackend.domain.chat.repository.ChatFeedbackRepository;
import org.refit.refitbackend.domain.chat.repository.ChatMessageRepository;
import org.refit.refitbackend.domain.chat.repository.ChatRoomRepository;
import org.refit.refitbackend.domain.report.dto.ReportReq;
import org.refit.refitbackend.domain.report.dto.ReportRes;
import org.refit.refitbackend.domain.report.entity.Report;
import org.refit.refitbackend.domain.report.entity.enums.ReportStatus;
import org.refit.refitbackend.domain.report.repository.ReportRepository;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserSkillRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final RestTemplate restTemplate;
    private final ReportRepository reportRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final ChatFeedbackAnswerRepository chatFeedbackAnswerRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserSkillRepository userSkillRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.base-url:https://dev.re-fit.kr/api/ai}")
    private String aiBaseUrl;

    @Transactional
    public ReportRes.ReportId create(Long userId, ReportReq.Create request) {
        ChatRoom room = chatRoomRepository.findByIdAndUserId(request.chatRoomId(), userId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (reportRepository.existsByChatRoomIdAndStatusIn(
                room.getId(), List.of(ReportStatus.PROCESSING, ReportStatus.COMPLETED)
        )) {
            throw new CustomException(ExceptionType.REPORT_ALREADY_EXISTS);
        }

        Long resumeId = room.getResumeId();
        Long chatRequestId = room.getChatRequestId();
        if (resumeId == null || chatRequestId == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        ChatFeedback feedback = chatFeedbackRepository.findDetailByChatRoomId(room.getId())
                .orElseThrow(() -> new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING));
        List<ChatFeedbackAnswer> feedbackAnswers = chatFeedbackAnswerRepository.findByChatFeedbackIdOrderByQuestion(feedback.getId());

        String jobPostUrl = room.getJobPostUrl();
        if (jobPostUrl == null || jobPostUrl.isBlank()) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }
        Long jobPostId = requestJobPostParse(jobPostUrl);
        Map<String, Object> aiResultJson = requestReportGenerate(resumeId, jobPostId, room.getId(), feedbackAnswers, userId);

        Report report = reportRepository.save(Report.builder()
                .userId(userId)
                .expertId(room.getReceiver().getId())
                .chatRoomId(room.getId())
                .chatFeedbackId(feedback.getId())
                .chatRequestId(chatRequestId)
                .resumeId(resumeId)
                .title("AI 리포트")
                .status(ReportStatus.COMPLETED)
                .resultJson(aiResultJson)
                .jobPostUrl(jobPostUrl)
                .build());

        return new ReportRes.ReportId(report.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoGenerateFromFeedback(Long userId, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));

        if (!room.getRequester().getId().equals(userId)) {
            return;
        }
        if (reportRepository.existsByChatRoomIdAndStatusIn(
                room.getId(), List.of(ReportStatus.PROCESSING, ReportStatus.COMPLETED)
        )) {
            return;
        }

        Long resumeId = room.getResumeId();
        Long chatRequestId = room.getChatRequestId();
        String jobPostUrl = room.getJobPostUrl();
        if (resumeId == null || chatRequestId == null || jobPostUrl == null || jobPostUrl.isBlank()) {
            return;
        }

        ChatFeedback feedback = chatFeedbackRepository.findDetailByChatRoomId(room.getId())
                .orElseThrow(() -> new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING));
        List<ChatFeedbackAnswer> feedbackAnswers =
                chatFeedbackAnswerRepository.findByChatFeedbackIdOrderByQuestion(feedback.getId());

        Long jobPostId = requestJobPostParse(jobPostUrl);
        Map<String, Object> aiResultJson = requestReportGenerate(resumeId, jobPostId, room.getId(), feedbackAnswers, userId);

        reportRepository.save(Report.builder()
                .userId(userId)
                .expertId(room.getReceiver().getId())
                .chatRoomId(room.getId())
                .chatFeedbackId(feedback.getId())
                .chatRequestId(chatRequestId)
                .resumeId(resumeId)
                .title("AI 리포트")
                .status(ReportStatus.COMPLETED)
                .resultJson(aiResultJson)
                .jobPostUrl(jobPostUrl)
                .build());
    }

    public ReportRes.ReportListResponse listMyReports(Long userId) {
        List<ReportRes.ReportListItem> items = reportRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReportRes.ReportListItem::from)
                .toList();
        return new ReportRes.ReportListResponse(items);
    }

    public ReportRes.ReportDetail getDetail(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.REPORT_NOT_FOUND));

        return new ReportRes.ReportDetail(
                report.getId(),
                report.getUserId(),
                report.getExpertId(),
                report.getChatRoomId(),
                report.getChatFeedbackId(),
                report.getChatRequestId(),
                report.getResumeId(),
                report.getTitle(),
                report.getStatus().name(),
                objectMapper.valueToTree(report.getResultJson()),
                report.getJobPostUrl(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    @Transactional
    public void delete(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.REPORT_NOT_FOUND));

        if (report.getStatus() == ReportStatus.PROCESSING) {
            throw new CustomException(ExceptionType.REPORT_DELETE_NOT_ALLOWED);
        }
        reportRepository.delete(report);
    }

    @Transactional
    public void retry(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.REPORT_NOT_FOUND));

        if (report.getStatus() == ReportStatus.PROCESSING) {
            throw new CustomException(ExceptionType.REPORT_ALREADY_PROCESSING);
        }
        if (report.getStatus() != ReportStatus.FAILED) {
            throw new CustomException(ExceptionType.REPORT_STATUS_NOT_FAILED);
        }

        report.markProcessing();
        try {
            ChatRoom room = chatRoomRepository.findById(report.getChatRoomId())
                    .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));
            ChatFeedback feedback = chatFeedbackRepository.findDetailByChatRoomId(room.getId())
                    .orElseThrow(() -> new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING));
            List<ChatFeedbackAnswer> feedbackAnswers = chatFeedbackAnswerRepository.findByChatFeedbackIdOrderByQuestion(feedback.getId());

            Long jobPostId = requestJobPostParse(report.getJobPostUrl());
            Map<String, Object> aiResultJson = requestReportGenerate(report.getResumeId(), jobPostId, room.getId(), feedbackAnswers, userId);
            report.markCompleted(aiResultJson);
        } catch (CustomException e) {
            report.markFailed();
            throw e;
        } catch (Exception e) {
            report.markFailed();
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private Long requestJobPostParse(String jobPostUrl) {
        String normalizedUrl = normalizeJobPostUrl(jobPostUrl);
        Map<String, Object> payload = buildJobPostParsePayload(normalizedUrl);
        return requestJobPostParseFromEndpoint("/repo/job-post", payload);
    }

    private Map<String, Object> buildJobPostParsePayload(String jobPostUrl) {
        String source = detectSource(jobPostUrl);
        Map<String, Object> payload = new HashMap<>();
        payload.put("job_url", jobPostUrl);
        if (source != null && !source.isBlank()) {
            payload.put("source", source);
        }
        return payload;
    }

    private String normalizeJobPostUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        if (normalized.startsWith("<") && normalized.endsWith(">") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            if (normalized.length() > 1) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        return normalized;
    }

    private String detectSource(String url) {
        if (url == null) {
            return null;
        }
        String lower = url.toLowerCase();
        if (lower.contains("saramin")) {
            return "saramin";
        }
        if (lower.contains("jobkorea")) {
            return "jobkorea";
        }
        if (lower.contains("wanted")) {
            return "wanted";
        }
        if (lower.contains("jumpit")) {
            return "jumpit";
        }
        return null;
    }

    private Long requestJobPostParseFromEndpoint(String path, Map<String, Object> payload) {
        String endpoint = UriComponentsBuilder.fromUriString(aiBaseUrl)
                .path(path)
                .toUriString();
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {}
            );
            return extractJobPostId(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("AI job-post parse request failed. status={}, payload={}, response={}",
                    e.getStatusCode(), payload, e.getResponseBodyAsString());
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private Long extractJobPostId(Map<String, Object> body) {
        if (body == null || !"OK".equals(String.valueOf(body.get("code")))) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> raw)) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        Object jobPostId = raw.get("job_post_id");
        if (jobPostId == null) {
            jobPostId = raw.get("jobposting_id");
        }
        if (jobPostId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(jobPostId));
    }

    private Map<String, Object> requestReportGenerate(
            Long resumeId,
            Long jobPostId,
            Long chatRoomId,
            List<ChatFeedbackAnswer> feedbackAnswers,
            Long userId
    ) {
        return requestReportGenerateFromEndpoint("/repo/generate", resumeId, jobPostId, chatRoomId, feedbackAnswers, userId);
    }

    private Map<String, Object> requestReportGenerateFromEndpoint(
            String path,
            Long resumeId,
            Long jobPostId,
            Long chatRoomId,
            List<ChatFeedbackAnswer> feedbackAnswers,
            Long userId
    ) {
        try {
            String endpoint = UriComponentsBuilder.fromUriString(aiBaseUrl)
                    .path(path)
                    .toUriString();

            Map<String, Object> payload = new HashMap<>();
            payload.put("resume_id", resumeId);
            payload.put("job_post_id", jobPostId);
            payload.put("user_skills", loadUserSkillNames(userId));
            payload.put("mentor_feedback", mapMentorFeedback(feedbackAnswers));
            payload.put("chat_messages", loadChatMessages(chatRoomId));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null || !"OK".equals(String.valueOf(body.get("code")))) {
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            Object data = body.get("data");
            if (!(data instanceof Map<?, ?> raw)) {
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> mapped = (Map<String, Object>) raw;
            return mapped;
        }
        catch (HttpStatusCodeException e) {
            log.warn("AI report generate request failed. path={}, status={}, response={}",
                    path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private List<Map<String, Object>> loadChatMessages(Long chatRoomId) {
        List<ChatMessage> messages = chatMessageRepository.findAllByChatIdOrderBySequence(chatRoomId);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("message_id", message.getId());
            row.put("chat_id", message.getChatRoom().getId());
            row.put("room_sequence", message.getRoomSequence());

            Map<String, Object> sender = new HashMap<>();
            sender.put("user_id", message.getSender().getId());
            sender.put("nickname", message.getSender().getNickname());
            sender.put("profile_image_url", message.getSender().getProfileImageUrl());
            sender.put("user_type", message.getSender().getUserType().name());
            row.put("sender", sender);

            row.put("message_type", message.getMessageType().name());
            row.put("content", message.getContent());
            row.put("client_message_id", message.getClientMessageId());
            row.put("created_at", message.getCreatedAt() == null ? null : message.getCreatedAt().toString());
            mapped.add(row);
        }
        return mapped;
    }

    private List<String> loadUserSkillNames(Long userId) {
        return userSkillRepository.findAllByUserIdIn(List.of(userId)).stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(UserSkill::getSkill)
                .map(skill -> skill.getName())
                .toList();
    }

    private Map<String, Object> mapMentorFeedback(List<ChatFeedbackAnswer> answers) {
        Map<String, String> byKey = new HashMap<>();
        for (ChatFeedbackAnswer answer : answers) {
            byKey.put(answer.getQuestion().getQuestionKey(), answer.getAnswerValue());
        }

        List<String> keyRequirements = splitCsv(byKey.get("step1_core_requirements"));
        List<Map<String, Object>> assessments = new ArrayList<>();
        assessments.add(buildAssessment(keyRequirements, 0, byKey.get("step2_req1_status"), byKey.get("step2_req1_reason")));
        assessments.add(buildAssessment(keyRequirements, 1, byKey.get("step2_req2_status"), byKey.get("step2_req2_reason")));
        assessments.add(buildAssessment(keyRequirements, 2, byKey.get("step2_req3_status"), byKey.get("step2_req3_reason")));

        Map<String, Object> mentor = new HashMap<>();
        mentor.put("key_requirements", keyRequirements);
        mentor.put("requirement_assessments", assessments);
        mentor.put("strengths", splitCsv(byKey.get("step3_strengths")));
        mentor.put("strengths_reason", safeText(byKey.get("step3_reason")));
        mentor.put("improvements", splitCsv(byKey.get("step4_improvements")));
        mentor.put("improvements_reason", safeText(byKey.get("step4_reason")));
        mentor.put("action_items", List.of(
                safeText(byKey.get("step5_action_1")),
                safeText(byKey.get("step5_action_2"))
        ));
        mentor.put("job_fit", normalizeFitLevel(byKey.get("step6_job_fit")));
        mentor.put("pass_probability", normalizeFitLevel(byKey.get("step7_doc_pass")));
        mentor.put("overall_comment", safeText(byKey.get("step8_summary")));
        return mentor;
    }

    private Map<String, Object> buildAssessment(List<String> requirements, int idx, String fulfillment, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("requirement", requirements.size() > idx ? requirements.get(idx) : "요구사항 " + (idx + 1));
        item.put("fulfillment", normalizeFulfillment(fulfillment));
        item.put("reason", safeText(reason));
        return item;
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeFulfillment(String value) {
        String normalized = safeText(value).replace(" ", "");
        if ("충족".equals(normalized) || "부분충족".equals(normalized) || "미충족".equals(normalized)) {
            return normalized;
        }
        return "미충족";
    }

    private String normalizeFitLevel(String value) {
        String normalized = safeText(value).replace(" ", "");
        if ("상".equals(normalized) || "중".equals(normalized) || "하".equals(normalized)) {
            return normalized;
        }
        return "중";
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] tokens = value.split("\\s*,\\s*");
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

}
