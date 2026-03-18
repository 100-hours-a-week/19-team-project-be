package org.refit.refitbackend.domain.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.agent.dto.AgentReq;
import org.refit.refitbackend.domain.agent.dto.AgentRes;
import org.refit.refitbackend.domain.agent.entity.AgentMessage;
import org.refit.refitbackend.domain.agent.entity.AgentMessageRole;
import org.refit.refitbackend.domain.agent.entity.AgentSession;
import org.refit.refitbackend.domain.agent.repository.AgentMessageRepository;
import org.refit.refitbackend.domain.agent.repository.AgentSessionRepository;
import org.refit.refitbackend.global.error.CustomException;
import org.refit.refitbackend.global.error.ExceptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AgentSessionRepository agentSessionRepository;
    private final AgentMessageRepository agentMessageRepository;

    @Value("${ai.base-url:https://dev.re-fit.kr/api/ai}")
    private String aiBaseUrl;

    @Value("${ai.reply-timeout-seconds:120}")
    private long replyTimeoutSeconds;

    public AgentRes.SessionInfo createSession(Long userId) {
        String url = aiBaseUrl + "/agent/sessions";
        JsonNode root = restTemplate.postForObject(url, HttpEntity.EMPTY, JsonNode.class);
        JsonNode data = requireData(root);
        AgentRes.SessionInfo info = toSessionInfo(data);

        if (info.sessionId() == null || info.sessionId().isBlank()) {
            throw new CustomException(ExceptionType.AI_SERVER_ERROR);
        }

        agentSessionRepository.save(new AgentSession(info.sessionId(), userId));
        return info;
    }

    public List<AgentRes.SessionInfo> listSessions(Long userId) {
        return agentSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toSessionInfo)
                .toList();
    }

    public AgentRes.SessionInfo getSession(Long userId, String sessionId) {
        AgentSession session = findOwnedSession(userId, sessionId);
        return toSessionInfo(session);
    }

    public List<AgentRes.MessageInfo> getMessages(Long userId, String sessionId) {
        findOwnedSession(userId, sessionId);

        return agentMessageRepository.findBySessionIdAndUserIdOrderByIdAsc(sessionId, userId)
                .stream()
                .map(this::toMessageInfo)
                .toList();
    }

    @Transactional
    public AgentRes.MessageFeedbackInfo updateMessageFeedback(Long userId, Long messageId, AgentReq.MessageFeedbackRequest request) {
        if (request == null) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        AgentMessage message = agentMessageRepository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.AI_CHAT_NOT_FOUND));

        if (message.getRole() != AgentMessageRole.ASSISTANT) {
            throw new CustomException(ExceptionType.INVALID_REQUEST);
        }

        message.updateFeedback(request.feedback());
        return new AgentRes.MessageFeedbackInfo(message.getId(), message.getFeedback());
    }

    public SseEmitter replyStream(Long userId, AgentReq.ReplyRequest request) {
        String sessionId = resolveSessionId(userId, request.sessionId());
        SseEmitter emitter = new SseEmitter(replyTimeoutSeconds * 1000);

        CompletableFuture.runAsync(() -> {
            try {
                saveUserMessage(sessionId, userId, request.message());
                increaseMessageCount(sessionId, userId);
                StringBuilder assistantMessage = new StringBuilder();
                String[] latestIntent = new String[1];
                JsonNode[] latestCards = new JsonNode[1];

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("session_id", sessionId);
                payload.put("message", request.message());
                payload.put("top_k", request.normalizedTopK());

                String body = objectMapper.writeValueAsString(payload);

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(aiBaseUrl + "/agent/reply"))
                        .timeout(Duration.ofSeconds(replyTimeoutSeconds))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<InputStream> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 400) {
                    String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    sendEvent(emitter, "error", Map.of("message", "Agent server error", "status", response.statusCode(), "body", err));
                    sendEvent(emitter, "done", Map.of());
                    emitter.complete();
                    return;
                }

                streamSse(sessionId, userId, response.body(), emitter, assistantMessage, latestIntent, latestCards);
            } catch (Exception e) {
                log.warn("Agent SSE relay failed", e);
                try {
                    sendEvent(emitter, "error", Map.of("message", e.getMessage() == null ? "Agent stream failed" : e.getMessage()));
                    sendEvent(emitter, "done", Map.of());
                } catch (Exception ignored) {
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    private void streamSse(
            String sessionId,
            Long userId,
            InputStream inputStream,
            SseEmitter emitter,
            StringBuilder assistantMessage,
            String[] latestIntent,
            JsonNode[] latestCards
    ) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            String eventName = null;
            StringBuilder dataBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    if (eventName != null) {
                        Object data = parseEventData(dataBuilder.toString());
                        captureState(eventName, data, assistantMessage, latestIntent, latestCards);

                        if ("done".equals(eventName)) {
                            AgentMessage saved = persistAssistantMessage(
                                    sessionId,
                                    userId,
                                    assistantMessage.toString(),
                                    latestIntent[0],
                                    latestCards[0]
                            );
                            updateSessionMetadata(sessionId, userId, latestIntent[0]);
                            sendAssistantMessageSavedEvent(emitter, sessionId, saved);
                            sendEvent(emitter, "done", data);
                            emitter.complete();
                            return;
                        }

                        sendEvent(emitter, eventName, data);
                    }
                    eventName = null;
                    dataBuilder.setLength(0);
                    continue;
                }

                if (line.startsWith("event:")) {
                    eventName = line.substring(6).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    if (eventName == null) {
                        eventName = "message";
                    }
                    if (!dataBuilder.isEmpty()) {
                        dataBuilder.append('\n');
                    }
                    dataBuilder.append(line.substring(5).trim());
                }
            }
        }

        AgentMessage saved = persistAssistantMessage(
                sessionId,
                userId,
                assistantMessage.toString(),
                latestIntent[0],
                latestCards[0]
        );
        updateSessionMetadata(sessionId, userId, latestIntent[0]);
        sendAssistantMessageSavedEvent(emitter, sessionId, saved);
        sendEvent(emitter, "done", Map.of());
        emitter.complete();
    }

    private void captureState(
            String eventName,
            Object data,
            StringBuilder assistantMessage,
            String[] latestIntent,
            JsonNode[] latestCards
    ) {
        if (data instanceof JsonNode node) {
            if ("intent".equals(eventName)) {
                JsonNode intent = node.get("intent");
                if (intent != null && !intent.isNull()) {
                    latestIntent[0] = intent.asText();
                }
            }

            if ("text".equals(eventName)) {
                JsonNode chunk = node.get("chunk");
                if (chunk != null && !chunk.isNull()) {
                    assistantMessage.append(chunk.asText());
                }
            }

            if ("cards".equals(eventName)) {
                latestCards[0] = node;
            }
        }
    }

    private String resolveSessionId(Long userId, String requestedSessionId) {
        if (requestedSessionId == null || requestedSessionId.isBlank()) {
            return createSession(userId).sessionId();
        }

        findOwnedSession(userId, requestedSessionId);
        return requestedSessionId;
    }

    private AgentSession findOwnedSession(Long userId, String sessionId) {
        return agentSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new CustomException(ExceptionType.AI_CHAT_NOT_FOUND));
    }

    private void saveUserMessage(String sessionId, Long userId, String message) {
        agentMessageRepository.save(AgentMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .role(AgentMessageRole.USER)
                .content(message)
                .build());
    }

    private AgentMessage persistAssistantMessage(
            String sessionId,
            Long userId,
            String message,
            String latestIntent,
            JsonNode latestCards
    ) {
        if (message == null || message.isBlank()) {
            return null;
        }

        AgentMessage saved = agentMessageRepository.save(AgentMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .role(AgentMessageRole.ASSISTANT)
                .content(message)
                .metadataJson(buildAssistantMetadataJson(latestIntent, latestCards))
                .build());
        increaseMessageCount(sessionId, userId);
        return saved;
    }

    private String buildAssistantMetadataJson(String latestIntent, JsonNode latestCards) {
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (latestIntent != null && !latestIntent.isBlank()) {
                metadata.put("intent", latestIntent);
            }
            if (latestCards != null && !latestCards.isNull()) {
                metadata.put("cards", latestCards.get("cards") != null ? latestCards.get("cards") : latestCards);
            }
            if (metadata.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize assistant metadata", e);
            return null;
        }
    }

    private void sendAssistantMessageSavedEvent(SseEmitter emitter, String sessionId, AgentMessage saved) throws Exception {
        if (saved == null) {
            return;
        }
        sendEvent(emitter, "message_saved", Map.of(
                "session_id", sessionId,
                "message_id", saved.getId()
        ));
    }

    private void updateSessionMetadata(String sessionId, Long userId, String lastIntent) {
        AgentSession session = findOwnedSession(userId, sessionId);
        session.updateLastIntent(lastIntent);
        agentSessionRepository.save(session);
    }

    private void increaseMessageCount(String sessionId, Long userId) {
        AgentSession session = findOwnedSession(userId, sessionId);
        session.increaseMessageCount();
        agentSessionRepository.save(session);
    }

    private Object parseEventData(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private JsonNode parseMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(metadataJson);
        } catch (Exception e) {
            log.warn("Failed to parse agent message metadata", e);
            return null;
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws Exception {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private JsonNode requireData(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode data = root.get("data");
        return data == null || data.isNull() ? null : data;
    }

    private AgentRes.SessionInfo toSessionInfo(JsonNode data) {
        if (data == null || data.isNull()) {
            return new AgentRes.SessionInfo(null, null, 0, null);
        }
        return new AgentRes.SessionInfo(
                asText(data, "session_id"),
                asText(data, "created_at"),
                asInt(data, "message_count"),
                asText(data, "last_intent")
        );
    }

    private AgentRes.SessionInfo toSessionInfo(AgentSession session) {
        return new AgentRes.SessionInfo(
                session.getSessionId(),
                session.getCreatedAt() == null ? null : session.getCreatedAt().toString(),
                session.getMessageCount(),
                session.getLastIntent()
        );
    }

    private AgentRes.MessageInfo toMessageInfo(AgentMessage message) {
        return new AgentRes.MessageInfo(
                message.getId(),
                message.getSessionId(),
                message.getRole().name(),
                message.getContent(),
                message.getFeedback(),
                parseMetadataJson(message.getMetadataJson()),
                message.getCreatedAt() == null ? null : message.getCreatedAt().toString()
        );
    }

    private String asText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }

    private int asInt(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? 0 : value.asInt();
    }
}
