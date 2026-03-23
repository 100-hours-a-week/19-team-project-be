package org.refit.refitbackend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = {
        "app.kafka.enabled",
        "app.notification.async.enabled",
        "app.notification.outbox.enabled"
}, havingValue = "true")
public class NotificationOutboxSchedulingConfig {
}
