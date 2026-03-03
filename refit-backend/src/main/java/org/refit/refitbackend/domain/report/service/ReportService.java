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
import org.refit.refitbackend.domain.jobposting.entity.JobPost;
import org.refit.refitbackend.domain.jobposting.entity.JobPostCrawlLog;
import org.refit.refitbackend.domain.jobposting.entity.enums.CrawlStatus;
import org.refit.refitbackend.domain.jobposting.repository.JobPostCrawlLogRepository;
import org.refit.refitbackend.domain.jobposting.repository.JobPostRepository;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.domain.report.dto.ReportReq;
import org.refit.refitbackend.domain.report.dto.ReportRes;
import org.refit.refitbackend.domain.report.entity.Report;
import org.refit.refitbackend.domain.report.entity.enums.ReportStatus;
import org.refit.refitbackend.domain.report.repository.ReportRepository;
import org.refit.refitbackend.domain.task.entity.Task;
import org.refit.refitbackend.domain.task.entity.enums.TaskStatus;
import org.refit.refitbackend.domain.task.entity.enums.TaskTargetType;
import org.refit.refitbackend.domain.task.entity.enums.TaskType;
import org.refit.refitbackend.domain.task.kafka.TaskEventPublisher;
import org.refit.refitbackend.domain.task.kafka.event.ReportGenerateRequestedEvent;
import org.refit.refitbackend.domain.task.repository.TaskRepository;
import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.refit.refitbackend.domain.user.repository.UserSkillRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

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
    private final JobPostRepository jobPostRepository;
    private final JobPostCrawlLogRepository jobPostCrawlLogRepository;
    private final UserSkillRepository userSkillRepository;
    private final NotificationService notificationService;
    private final ObjectProvider<TaskEventPublisher> taskEventPublisherProvider;
    private final TaskRepository taskRepository;
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
        Report report = reportRepository.save(Report.builder()
                .userId(userId)
                .expertId(room.getReceiver().getId())
                .chatRoomId(room.getId())
                .chatFeedbackId(feedback.getId())
                .chatRequestId(chatRequestId)
                .resumeId(resumeId)
                .title("AI 리포트")
                .status(ReportStatus.PROCESSING)
                .jobPostUrl(jobPostUrl)
                .build());
        Task task = createReportTask(userId, report.getId());

        try {
            TaskEventPublisher publisher = taskEventPublisherProvider.getIfAvailable();
            if (publisher == null) {
                report.markFailed();
                task.markFailed("KAFKA_DISABLED");
                taskRepository.save(task);
                log.error("[REPORT_ASYNC] publisher unavailable. reportId={}, taskId={}", report.getId(), task.getId());
                return new ReportRes.ReportId(report.getId());
            }
            publisher.publishReportGenerateRequested(
                    new ReportGenerateRequestedEvent(task.getId(), userId, report.getId(), room.getId())
            );
        } catch (Exception e) {
            report.markFailed();
            task.markFailed("KAFKA_PUBLISH_FAILED");
            taskRepository.save(task);
            log.error("[REPORT_ASYNC] publish failed. reportId={}, taskId={}", report.getId(), task.getId(), e);
            return new ReportRes.ReportId(report.getId());
        }

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
        Report report = reportRepository.save(Report.builder()
                .userId(userId)
                .expertId(room.getReceiver().getId())
                .chatRoomId(room.getId())
                .chatFeedbackId(feedback.getId())
                .chatRequestId(chatRequestId)
                .resumeId(resumeId)
                .title("AI 리포트")
                .status(ReportStatus.PROCESSING)
                .jobPostUrl(jobPostUrl)
                .build());
        Task task = createReportTask(userId, report.getId());

        try {
            TaskEventPublisher publisher = taskEventPublisherProvider.getIfAvailable();
            if (publisher == null) {
                report.markFailed();
                task.markFailed("KAFKA_DISABLED");
                taskRepository.save(task);
                log.error("[REPORT_ASYNC] auto publish skipped. publisher unavailable. reportId={}, taskId={}",
                        report.getId(), task.getId());
                return;
            }
            publisher.publishReportGenerateRequested(
                    new ReportGenerateRequestedEvent(task.getId(), userId, report.getId(), room.getId())
            );
        } catch (Exception e) {
            report.markFailed();
            task.markFailed("KAFKA_PUBLISH_FAILED");
            taskRepository.save(task);
            log.error("[REPORT_ASYNC] auto publish failed. reportId={}, taskId={}", report.getId(), task.getId(), e);
        }
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

    @Transactional
    public void processAsyncGenerateReportTask(ReportGenerateRequestedEvent event) {
        Report report = reportRepository.findById(event.reportId()).orElse(null);
        Task task = taskRepository.findById(event.taskId()).orElse(null);
        if (report == null) {
            log.warn("[REPORT_ASYNC] report not found. reportId={}", event.reportId());
            return;
        }
        if (task == null) {
            log.warn("[REPORT_ASYNC] task not found. taskId={}", event.taskId());
            return;
        }
        if (task.getStatus() != TaskStatus.PROCESSING) {
            log.info("[REPORT_ASYNC] skip task. taskId={}, status={}", task.getId(), task.getStatus());
            return;
        }
        if (report.getStatus() != ReportStatus.PROCESSING) {
            log.info("[REPORT_ASYNC] skip. reportId={}, status={}", report.getId(), report.getStatus());
            return;
        }

        try {
            ChatRoom room = chatRoomRepository.findById(report.getChatRoomId())
                    .orElseThrow(() -> new CustomException(ExceptionType.CHAT_ROOM_NOT_FOUND));
            ChatFeedback feedback = chatFeedbackRepository.findDetailByChatRoomId(room.getId())
                    .orElseThrow(() -> new CustomException(ExceptionType.FEEDBACK_ANSWER_MISSING));
            List<ChatFeedbackAnswer> feedbackAnswers =
                    chatFeedbackAnswerRepository.findByChatFeedbackIdOrderByQuestion(feedback.getId());

            Long jobPostId = requestJobPostParse(report.getJobPostUrl());
            Map<String, Object> aiResultJson = requestReportGenerate(
                    report.getResumeId(), jobPostId, room.getId(), feedbackAnswers, report.getUserId()
            );
            report.markCompleted(aiResultJson);
            task.markCompleted(toJson(Map.of("report_id", report.getId())));
            notifyReportCompletedSafely(report.getUserId());
        } catch (CustomException e) {
            if (isReportAsyncRetryable(e)) {
                log.warn("[REPORT_ASYNC] retryable failure. reportId={}, code={}",
                        report.getId(), e.getExceptionType().getCode());
                throw e;
            }
            report.markFailed();
            task.markFailed(e.getExceptionType().getCode());
            notifyReportFailedSafely(report.getUserId(), e.getExceptionType().getCode());
        } catch (Exception e) {
            log.error("[REPORT_ASYNC] retryable unexpected failure. reportId={}", report.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private Long requestJobPostParse(String jobPostUrl) {
        String normalizedUrl = normalizeJobPostUrl(jobPostUrl);
        String source = detectSource(normalizedUrl);
        Map<String, Object> payload = buildJobPostParsePayload(normalizedUrl);
        Map<String, Object> parsed = requestJobPostParseFromEndpoint("/repo/job-post", payload);
        Long jobPostId = extractJobPostIdFromParsed(parsed);
        mirrorJobPost(normalizedUrl, source, jobPostId, parsed);
        return jobPostId;
    }

    private Map<String, Object> buildJobPostParsePayload(String jobPostUrl) {
        String source = detectSource(jobPostUrl);
        Map<String, Object> payload = new HashMap<>();
        payload.put("job_url", jobPostUrl);
        payload.put("job_post_url", jobPostUrl);
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

    private void mirrorJobPost(String url, String source, Long sourceJobId, Map<String, Object> parsed) {
        if (url == null || url.isBlank() || sourceJobId == null) {
            return;
        }
        try {
            String resolvedSource = (source == null || source.isBlank()) ? "unknown" : source;
            String resolvedSourceJobId = String.valueOf(sourceJobId);
            String urlHash = sha256(url);

            JobPost jobPost = jobPostRepository.findBySourceAndSourceJobId(resolvedSource, resolvedSourceJobId)
                    .orElseGet(() -> jobPostRepository.findByUrlHash(urlHash).orElse(null));

            String title = firstNonBlank(readText(parsed, "title"), "채용 공고");
            String company = firstNonBlank(readText(parsed, "company"), "알 수 없음");
            String department = readText(parsed, "department");
            String employmentType = readText(parsed, "employment_type");
            String experienceRequired = readText(parsed, "experience_required");
            String educationRequired = readText(parsed, "education_required");
            String requirements = toJsonArray(parsed, "requirements");
            String preferences = toJsonArray(parsed, "preferences");
            String techStack = toJsonArray(parsed, "tech_stack");
            String responsibilities = toJsonArray(parsed, "responsibilities");
            String descriptionRaw = readText(parsed, "description_raw");

            if (jobPost == null) {
                jobPostRepository.save(JobPost.builder()
                        .source(resolvedSource)
                        .sourceJobId(resolvedSourceJobId)
                        .url(url)
                        .urlHash(urlHash)
                        .title(title)
                        .company(company)
                        .department(department)
                        .location(null)
                        .employmentType(employmentType)
                        .experienceRequired(experienceRequired)
                        .educationRequired(educationRequired)
                        .techStack(techStack)
                        .requirements(requirements)
                        .preferences(preferences)
                        .responsibilities(responsibilities)
                        .descriptionRaw(descriptionRaw)
                        .descriptionClean(null)
                        .postedAt(null)
                        .deadlineAt(null)
                        .isActive(true)
                        .crawledAt(LocalDateTime.now())
                        .build());
                return;
            }

            jobPost.updateFromCrawler(
                    firstNonBlank(title, defaultIfBlank(jobPost.getTitle(), "채용 공고")),
                    firstNonBlank(company, defaultIfBlank(jobPost.getCompany(), "알 수 없음")),
                    firstNonBlank(department, jobPost.getDepartment()),
                    firstNonBlank(employmentType, jobPost.getEmploymentType()),
                    firstNonBlank(experienceRequired, jobPost.getExperienceRequired()),
                    firstNonBlank(educationRequired, jobPost.getEducationRequired()),
                    defaultJson(firstNonBlank(requirements, jobPost.getRequirements())),
                    defaultJson(firstNonBlank(preferences, jobPost.getPreferences())),
                    defaultJson(firstNonBlank(techStack, jobPost.getTechStack())),
                    defaultJson(firstNonBlank(responsibilities, jobPost.getResponsibilities())),
                    firstNonBlank(descriptionRaw, jobPost.getDescriptionRaw())
            );
            jobPostRepository.save(jobPost);
        } catch (Exception e) {
            log.warn("[REPORT_ASYNC] job-post mirror save failed. source={}, sourceJobId={}, url={}",
                    source, sourceJobId, url, e);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String defaultJson(String value) {
        if (value == null || value.isBlank()) {
            return "[]";
        }
        return value;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> requestJobPostParseFromEndpoint(String path, Map<String, Object> payload) {
        String endpoint = UriComponentsBuilder.fromUriString(aiBaseUrl)
                .path(path)
                .toUriString();
        String jobUrl = String.valueOf(payload.getOrDefault("job_url", payload.getOrDefault("job_post_url", "")));
        String source = detectSource(jobUrl);
        JobPostCrawlLog crawlLog = jobPostCrawlLogRepository.save(JobPostCrawlLog.builder()
                .source(source == null || source.isBlank() ? "unknown" : source)
                .targetUrl(jobUrl)
                .status(CrawlStatus.FAILED)
                .startedAt(LocalDateTime.now())
                .build());
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> parsed = extractJobPostData(response.getBody());
            crawlLog.markSuccess(response.getStatusCode().value());
            jobPostCrawlLogRepository.save(crawlLog);
            return parsed;
        } catch (HttpStatusCodeException e) {
            log.warn("AI job-post parse request failed. status={}, payload={}, response={}",
                    e.getStatusCode(), payload, e.getResponseBodyAsString());
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

    private Map<String, Object> extractJobPostData(Map<String, Object> body) {
        if (body == null || !"OK".equals(String.valueOf(body.get("code")))) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> raw)) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) raw;
        Object jobPostId = parsed.get("job_post_id");
        if (jobPostId == null) {
            jobPostId = parsed.get("jobposting_id");
        }
        if (jobPostId == null) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        return parsed;
    }

    private Long extractJobPostIdFromParsed(Map<String, Object> parsed) {
        Object jobPostId = parsed.get("job_post_id");
        if (jobPostId == null) {
            jobPostId = parsed.get("jobposting_id");
        }
        if (jobPostId instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(jobPostId));
    }

    private String readText(Map<String, Object> parsed, String key) {
        Object value = parsed.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String toJsonArray(Map<String, Object> parsed, String key) {
        Object value = parsed.get(key);
        if (value == null) return "[]";
        if (value instanceof List<?> list) {
            String json = toJson(list);
            return (json == null || json.isBlank()) ? "[]" : json;
        }
        return "[]";
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary == null || primary.isBlank()) {
            return fallback;
        }
        return primary;
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

    private boolean isReportAsyncRetryable(CustomException e) {
        return e.getExceptionType() == ExceptionType.AI_SERVER_ERROR
                || e.getExceptionType() == ExceptionType.INTERNAL_SERVER_ERROR;
    }

    @Transactional
    public void markAsyncGenerateReportFailedFromDlq(String taskId, Long userId, Long reportId, String reasonCode) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            Task failedTask = Task.builder()
                    .id(taskId)
                    .userId(userId)
                    .type(TaskType.REPORT_GENERATION)
                    .status(TaskStatus.FAILED)
                    .progress(0)
                    .targetType(TaskTargetType.REPORT)
                    .targetId(reportId)
                    .build();
            failedTask.markFailed(reasonCode);
            taskRepository.save(failedTask);
        } else if (task.getStatus() == TaskStatus.PROCESSING) {
            task.markFailed(reasonCode);
            taskRepository.save(task);
        }
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return;
        }
        if (report.getStatus() != ReportStatus.PROCESSING) {
            return;
        }
        report.markFailed();
        notifyReportFailedSafely(report.getUserId(), reasonCode);
    }

    private void notifyReportCompletedSafely(Long userId) {
        try {
            notificationService.notifyReportGenerateCompleted(userId);
        } catch (Exception e) {
            log.warn("[REPORT] completion notification failed. userId={}", userId, e);
        }
    }

    private void notifyReportFailedSafely(Long userId, String reasonCode) {
        try {
            notificationService.notifyReportGenerateFailed(userId, reasonCode);
        } catch (Exception e) {
            log.warn("[REPORT] failure notification failed. userId={}, reasonCode={}", userId, reasonCode, e);
        }
    }

    private Task createReportTask(Long userId, Long reportId) {
        Task task = Task.builder()
                .id("task_report_" + UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .type(TaskType.REPORT_GENERATION)
                .status(TaskStatus.PROCESSING)
                .progress(0)
                .targetType(TaskTargetType.REPORT)
                .targetId(reportId)
                .build();
        return taskRepository.save(task);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

}
