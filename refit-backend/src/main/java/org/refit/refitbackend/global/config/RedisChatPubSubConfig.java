package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.domain.chat.realtime.RedisChatRealtimeSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.realtime.redis.enabled", havingValue = "true")
public class RedisChatPubSubConfig {

    @Bean
    public ChannelTopic chatRealtimeTopic(
            @org.springframework.beans.factory.annotation.Value("${app.chat.realtime.redis.channel:chat.message.broadcast}")
            String channel
    ) {
        return new ChannelTopic(channel);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisChatRealtimeSubscriber subscriber,
            ChannelTopic chatRealtimeTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, chatRealtimeTopic);
        return container;
    }
}

