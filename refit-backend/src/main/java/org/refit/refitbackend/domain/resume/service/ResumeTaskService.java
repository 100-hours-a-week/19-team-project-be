package org.refit.refitbackend.domain.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.notification.service.NotificationService;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.domain.task.entity.Task;
import org.refit.refitbackend.domain.task.entity.enums.TaskStatus;
import org.refit.refitbackend.domain.task.entity.enums.TaskTargetType;
import org.refit.refitbackend.domain.task.entity.enums.TaskType;
import org.refit.refitbackend.domain.task.kafka.TaskEventPublisher;
import org.refit.refitbackend.domain.task.kafka.event.ResumeParseRequestedEvent;
import org.refit.refitbackend.domain.task.repository.TaskRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.storage.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTaskService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final StorageClient storageClient;
    private final NotificationService notificationService;
    private final ObjectProvider<TaskEventPublisher> taskEventPublisherProvider;

    @Value("${ai.base-url:https://re-fit.kr/api/ai}")
    private String aiBaseUrl;

    public ResumeTaskRes.TaskResult parseSync(Long userId, ResumeTaskReq.Parse request) {
        if (!"sync".equalsIgnoreCase(request.mode())) {
            throw new CustomException(ExceptionType.RESUME_MODE_INVALID);
        }

        String fileUrl = request.fileUrl();
        validatePdfFileUrl(fileUrl);
        Task task = createResumeTask(userId, fileUrl, request.mode());
        String taskId = task.getId();

        JsonNode result;
        int resumeId = (int) (System.currentTimeMillis() % 1_000_000_000L);
        try {
            String aiFileUrl = resolveAiFileUrl(fileUrl);
            result = parseResume(resumeId, aiFileUrl);
        } catch (CustomException e) {
            task.markFailed(e.getExceptionType().getCode());
            taskRepository.save(task);
            notifyResumeFailedSafely(userId, taskId, e.getExceptionType().getCode());
            throw e;
        } catch (Exception e) {
            task.markFailed("INTERNAL_SERVER_ERROR");
            taskRepository.save(task);
            notifyResumeFailedSafely(userId, taskId, "INTERNAL_SERVER_ERROR");
            throw e;
        }

        ResumeTaskRes.ParsedResult parsedResult = toParsedResult(result);

        task.markCompleted(toJson(parsedResult));
        taskRepository.save(task);
        notifyResumeCompletedSafely(userId, taskId);

        return new ResumeTaskRes.TaskResult(
                taskId,
                TaskStatus.COMPLETED.name(),
                parsedResult
        );
    }

    public ResumeTaskRes.TaskResult createAsyncTask(Long userId, ResumeTaskReq.CreateTask request) {
        String fileUrl = request.fileUrl();
        validatePdfFileUrl(fileUrl);

        Task task = createResumeTask(userId, fileUrl, "async");
        try {
            TaskEventPublisher publisher = taskEventPublisherProvider.getIfAvailable();
            if (publisher == null) {
                task.markFailed("KAFKA_DISABLED");
                taskRepository.save(task);
                throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
            }
            publisher.publishResumeParseRequested(
                    new ResumeParseRequestedEvent(task.getId(), userId, fileUrl)
            );
        } catch (Exception e) {
            task.markFailed("KAFKA_PUBLISH_FAILED");
            taskRepository.save(task);
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }

        return new ResumeTaskRes.TaskResult(task.getId(), TaskStatus.PROCESSING.name(), null);
    }

    public ResumeTaskRes.TaskResult getTask(Long userId, String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ExceptionType.TASK_NOT_FOUND));
        if (!task.getUserId().equals(userId)) {
            throw new CustomException(ExceptionType.FORBIDDEN);
        }
        return new ResumeTaskRes.TaskResult(
                task.getId(),
                task.getStatus().name(),
                parseTaskResult(task.getResult())
        );
    }

    public void processAsyncParseTask(ResumeParseRequestedEvent event) {
        Task task = taskRepository.findById(event.taskId()).orElse(null);
        if (task == null) {
            log.warn("[RESUME_PARSE_ASYNC] task not found. taskId={}", event.taskId());
            return;
        }
        if (task.getStatus() != TaskStatus.PROCESSING) {
            log.info("[RESUME_PARSE_ASYNC] skip. taskId={}, status={}", task.getId(), task.getStatus());
            return;
        }

        try {
            JsonNode result = parseResume((int) (System.currentTimeMillis() % 1_000_000_000L), resolveAiFileUrl(event.fileUrl()));
            ResumeTaskRes.ParsedResult parsedResult = toParsedResult(result);
            task.markCompleted(toJson(parsedResult));
            taskRepository.save(task);
            notifyResumeCompletedSafely(task.getUserId(), task.getId());
        } catch (CustomException e) {
            if (isResumeAsyncRetryable(e)) {
                log.warn("[RESUME_PARSE_ASYNC] retryable failure. taskId={}, code={}",
                        task.getId(), e.getExceptionType().getCode());
                throw e;
            }
            task.markFailed(e.getExceptionType().getCode());
            taskRepository.save(task);
            notifyResumeFailedSafely(task.getUserId(), task.getId(), e.getExceptionType().getCode());
        } catch (Exception e) {
            log.error("[RESUME_PARSE_ASYNC] retryable unexpected failure. taskId={}", task.getId(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean isResumeAsyncRetryable(CustomException e) {
        return e.getExceptionType() == ExceptionType.AI_SERVER_ERROR
                || e.getExceptionType() == ExceptionType.INTERNAL_SERVER_ERROR
                || e.getExceptionType() == ExceptionType.RESUME_PARSE_FAILED;
    }

    @Transactional
    public void markAsyncParseTaskFailedFromDlq(String taskId, String reasonCode) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (task.getStatus() != TaskStatus.PROCESSING) {
            return;
        }
        task.markFailed(reasonCode);
        taskRepository.save(task);
        notifyResumeFailedSafely(task.getUserId(), task.getId(), reasonCode);
    }

    private JsonNode parseResume(int resumeId, String fileUrl) {
        try {
            String url = UriComponentsBuilder.fromUriString(aiBaseUrl + "/resumes/" + resumeId + "/parse")
                    .toUriString();

            Map<String, Object> payload = Map.of(
                    "file_url", fileUrl,
                    "enable_pii_masking", true
            );

            log.info("[RESUME_PARSE] request url={}, payload={}", url, payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            log.info("[RESUME_PARSE] response status={}, body={}", response.getStatusCode(), response.getBody());

            Map<String, Object> data = extractDataMap(response.getBody());
            Object status = data.get("status");
            if (status == null || !"COMPLETED".equals(status.toString())) {
                throw new CustomException(ExceptionType.RESUME_PARSE_FAILED);
            }
            Object result = data.get("result");
            return objectMapper.valueToTree(result);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("[RESUME_PARSE] failed", e);
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveAiFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new CustomException(ExceptionType.RESUME_FILE_URL_INVALID);
        }
        String normalized = fileUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("x-amz-signature") || normalized.contains("x-amz-algorithm")) {
            return fileUrl;
        }
        PresignedUrlResponse presigned = storageClient.getPresignedDownloadUrl(fileUrl);
        return presigned.presignedUrl();
    }

    private Map<String, Object> extractDataMap(Map<String, Object> response) {
        if (response == null || !response.containsKey("data")) {
            throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            //noinspection unchecked
            return (Map<String, Object>) map;
        }
        throw new CustomException(ExceptionType.INTERNAL_SERVER_ERROR);
    }

    private JsonNode mapContentJson(JsonNode content) {
        ObjectNode mapped = objectMapper.createObjectNode();

        mapped.set("careers", pickArray(content, "careers", "work_experience"));
        mapped.set("projects", pickArray(content, "projects"));
        mapped.set("education", pickArray(content, "education"));
        mapped.set("awards", pickArray(content, "awards"));
        mapped.set("certificates", pickArray(content, "certificates", "certifications"));
        mapped.set("activities", pickArray(content, "activities", "etc"));

        return mapped;
    }

    private ArrayNode pickArray(JsonNode content, String... keys) {
        if (content != null) {
            for (String key : keys) {
                JsonNode value = content.get(key);
                if (value != null && value.isArray()) {
                    return (ArrayNode) value;
                }
            }
        }
        return objectMapper.createArrayNode();
    }

    private void validatePdfFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new CustomException(ExceptionType.RESUME_FILE_URL_INVALID);
        }
        String normalized = fileUrl.toLowerCase();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.endsWith(".pdf")) {
            throw new CustomException(ExceptionType.RESUME_FILE_NOT_PDF);
        }
    }

    private Task createResumeTask(Long userId, String fileUrl, String mode) {
        String taskId = "task_resume_" + UUID.randomUUID().toString().replace("-", "");
        Task task = Task.builder()
                .id(taskId)
                .userId(userId)
                .type(TaskType.RESUME_EXTRACTION)
                .status(TaskStatus.PROCESSING)
                .fileUrl(fileUrl)
                .payload(toJson(Map.of("file_url", fileUrl, "mode", mode)))
                .targetType(TaskTargetType.RESUME)
                .build();
        return taskRepository.save(task);
    }

    private ResumeTaskRes.ParsedResult toParsedResult(JsonNode result) {
        JsonNode content = result.get("content_json");
        JsonNode isFresher = result.get("is_fresher");
        JsonNode educationLevel = result.get("education_level");
        JsonNode rawText = result.get("raw_text_excerpt");

        return new ResumeTaskRes.ParsedResult(
                isFresher != null && isFresher.asBoolean(),
                educationLevel != null ? educationLevel.asText() : null,
                mapContentJson(content),
                rawText != null ? rawText.asText() : null
        );
    }

    private ResumeTaskRes.ParsedResult parseTaskResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(resultJson, ResumeTaskRes.ParsedResult.class);
        } catch (Exception e) {
            log.warn("[RESUME_TASK] failed to parse task result. resultJsonLength={}",
                    resultJson != null ? resultJson.length() : 0);
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }

    private void notifyResumeCompletedSafely(Long userId, String taskId) {
        try {
            notificationService.notifyResumeParseCompleted(userId, taskId);
        } catch (Exception e) {
            log.warn("[RESUME_PARSE] completion notification failed. taskId={}", taskId, e);
        }
    }

    private void notifyResumeFailedSafely(Long userId, String taskId, String reasonCode) {
        try {
            notificationService.notifyResumeParseFailed(userId, taskId, reasonCode);
        } catch (Exception e) {
            log.warn("[RESUME_PARSE] failure notification failed. taskId={}, reason={}", taskId, reasonCode, e);
        }
    }
}
