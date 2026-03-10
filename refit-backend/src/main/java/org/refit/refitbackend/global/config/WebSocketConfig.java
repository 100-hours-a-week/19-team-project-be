package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.websocket.JwtChannelInterceptor;
import org.refit.refitbackend.global.websocket.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${app.websocket.inbound.core-pool-size:8}")
    private int inboundCorePoolSize;

    @Value("${app.websocket.inbound.max-pool-size:32}")
    private int inboundMaxPoolSize;

    @Value("${app.websocket.inbound.queue-capacity:2000}")
    private int inboundQueueCapacity;

    @Value("${app.websocket.outbound.core-pool-size:8}")
    private int outboundCorePoolSize;

    @Value("${app.websocket.outbound.max-pool-size:32}")
    private int outboundMaxPoolSize;

    @Value("${app.websocket.outbound.queue-capacity:2000}")
    private int outboundQueueCapacity;

    @Value("${app.websocket.transport.send-time-limit-ms:20000}")
    private int sendTimeLimitMs;

    @Value("${app.websocket.transport.send-buffer-size-limit-bytes:1048576}")
    private int sendBufferSizeLimitBytes;

    @Value("${app.websocket.transport.message-size-limit-bytes:131072}")
    private int messageSizeLimitBytes;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트로 메시지를 보낼 때 사용하는 prefix
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트에서 서버로 메시지를 보낼 때 사용하는 prefix
        config.setApplicationDestinationPrefixes("/app");

        // 특정 사용자에게 메시지 보낼 때 사용
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://re-fit.kr",
                        "https://dev.re-fit.kr",
                        "https://prod-v2.re-fit.kr"
                )
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();  // SockJS fallback 옵션 활성화

        // SockJS 없이 순수 WebSocket만 사용하는 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://re-fit.kr",
                        "https://dev.re-fit.kr",
                        "https://prod-v2.re-fit.kr"
                )
                .addInterceptors(jwtHandshakeInterceptor);

        registry.addEndpoint("/api/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://re-fit.kr",
                        "https://dev.re-fit.kr",
                        "https://prod-v2.re-fit.kr"
                )
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();

        registry.addEndpoint("/api/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "https://re-fit.kr",
                        "https://dev.re-fit.kr",
                        "https://prod-v2.re-fit.kr"
                )
                .addInterceptors(jwtHandshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 인증 인터셉터 등록 + inbound worker 튜닝
        registration.interceptors(jwtChannelInterceptor);
        registration.taskExecutor()
                .corePoolSize(inboundCorePoolSize)
                .maxPoolSize(inboundMaxPoolSize)
                .queueCapacity(inboundQueueCapacity);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // outbound worker 튜닝
        registration.taskExecutor()
                .corePoolSize(outboundCorePoolSize)
                .maxPoolSize(outboundMaxPoolSize)
                .queueCapacity(outboundQueueCapacity);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setSendTimeLimit(sendTimeLimitMs);
        registry.setSendBufferSizeLimit(sendBufferSizeLimitBytes);
        registry.setMessageSizeLimit(messageSizeLimitBytes);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        JsonMapper mapper = JsonMapper.builder()
                .build();
        messageConverters.add(new JacksonJsonMessageConverter(mapper));
        return false;
    }
}
