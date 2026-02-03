package org.refit.refitbackend.domain.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskReq;
import org.refit.refitbackend.domain.resume.dto.ResumeTaskRes;
import org.refit.refitbackend.domain.task.entity.Task;
import org.refit.refitbackend.domain.task.entity.enums.TaskStatus;
import org.refit.refitbackend.domain.task.entity.enums.TaskTargetType;
import org.refit.refitbackend.domain.task.entity.enums.TaskType;
import org.refit.refitbackend.domain.task.repository.TaskRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.storage.PresignedUrlResponse;
import org.refit.refitbackend.global.storage.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    @Value("${ai.base-url:https://re-fit.kr/api/ai}")
    private String aiBaseUrl;

    public ResumeTaskRes.TaskResult parseSync(Long userId, ResumeTaskReq.Parse request) {
        if (!"sync".equalsIgnoreCase(request.mode())) {
            throw new CustomException(ExceptionType.RESUME_MODE_INVALID);
        }

        String fileUrl = request.fileUrl();
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

        String taskId = "task_resume_" + UUID.randomUUID().toString().replace("-", "");
        Task task = Task.builder()
                .id(taskId)
                .userId(userId)
                .type(TaskType.RESUME_EXTRACTION)
                .status(TaskStatus.PROCESSING)
                .fileUrl(fileUrl)
                .payload(toJson(Map.of("file_url", fileUrl, "mode", request.mode())))
                .targetType(TaskTargetType.RESUME)
                .build();
        taskRepository.save(task);

        JsonNode result;
        int resumeId = (int) (System.currentTimeMillis() % 1_000_000_000L);
        try {
            String aiFileUrl = resolveAiFileUrl(fileUrl);
            result = parseResume(resumeId, aiFileUrl);
        } catch (CustomException e) {
            task.markFailed(e.getExceptionType().getCode());
            taskRepository.save(task);
            throw e;
        } catch (Exception e) {
            task.markFailed("INTERNAL_SERVER_ERROR");
            taskRepository.save(task);
            throw e;
        }

        JsonNode content = result.get("content_json");
        JsonNode isFresher = result.get("is_fresher");
        JsonNode educationLevel = result.get("education_level");
        JsonNode rawText = result.get("raw_text_excerpt");

        ResumeTaskRes.ParsedResult parsedResult = new ResumeTaskRes.ParsedResult(
                isFresher != null && isFresher.asBoolean(),
                educationLevel != null ? educationLevel.asText() : null,
                mapContentJson(content),
                rawText != null ? rawText.asText() : null
        );

        task.markCompleted(toJson(parsedResult));
        taskRepository.save(task);

        return new ResumeTaskRes.TaskResult(
                taskId,
                TaskStatus.COMPLETED.name(),
                parsedResult
        );
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new CustomException(ExceptionType.INVALID_JSON);
        }
    }
}