package org.refit.refitbackend.domain.jobposting.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskReq;
import org.refit.refitbackend.domain.jobposting.dto.JobPostTaskRes;
import org.refit.refitbackend.domain.jobposting.entity.JobPost;
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
        try {
            String endpoint = UriComponentsBuilder
                    .fromUriString(aiBaseUrl)
                    .path("/repo/job-post")
                    .toUriString();

            Map<String, Object> payload = new HashMap<>();
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
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            Object code = body.get("code");
            if (code == null || !"OK".equals(code.toString())) {
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }
            Object data = body.get("data");
            if (!(data instanceof Map<?, ?> dataMapRaw)) {
                throw new CustomException(ExceptionType.AI_SERVER_ERROR);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) dataMapRaw;
            return new JobPostTaskReq.ParsedData(
                    extractJobPostId(dataMap),
                    toText(dataMap.get("title")),
                    toText(dataMap.get("company")),
                    nullableText(dataMap.get("department")),
                    nullableText(dataMap.get("employment_type")),
                    nullableText(dataMap.get("experience_required")),
                    nullableText(dataMap.get("education_required")),
                    toStringList(dataMap.get("requirements")),
                    toStringList(dataMap.get("preferences")),
                    toStringList(dataMap.get("tech_stack")),
                    toStringList(dataMap.get("responsibilities"))
            );
        } catch (HttpStatusCodeException e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
    }

    private Long upsertJobPostAndCompleteTask(Task task, JobPostTaskReq.ParsedData parsed) {
        String source = extractSourceFromPayload(task.getPayload());
        String url = extractUrlFromPayload(task.getPayload());
        String sourceJobId = String.valueOf(parsed.jobPostId());

        JobPost jobPost = jobPostRepository.findBySourceAndSourceJobId(source, sourceJobId)
                .orElseGet(() -> JobPost.builder()
                        .source(source)
                        .sourceJobId(sourceJobId)
                        .url(url)
                        .urlHash(sha256(url))
                        .title(parsed.title())
                        .company(parsed.company())
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
                    parsed.title(),
                    parsed.company(),
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

    private String toText(Object value) {
        if (value == null) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        String text = value.toString();
        if (text.isBlank()) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }
        return text;
    }

    private String nullableText(Object value) {
        return value == null ? null : value.toString();
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
}
