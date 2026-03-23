package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.notification.realtime.RedisNotificationRealtimeSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.realtime.redis.enabled", havingValue = "true")
public class RedisNotificationPubSubConfig {

    @Bean
    public ChannelTopic notificationRealtimeTopic(
            @Value("${app.notification.realtime.redis.channel:notification.event.broadcast}")
            String channel
    ) {
        return new ChannelTopic(channel);
    }

    @Bean
    public RedisMessageListenerContainer notificationRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisNotificationRealtimeSubscriber subscriber,
            ChannelTopic notificationRealtimeTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, notificationRealtimeTopic);
        return container;
    }
}
