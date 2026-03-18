package org.refit.refitbackend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.chat.persistence.async.enabled"}, havingValue = "true")
public class ChatPersistenceSchedulingConfig {
}

