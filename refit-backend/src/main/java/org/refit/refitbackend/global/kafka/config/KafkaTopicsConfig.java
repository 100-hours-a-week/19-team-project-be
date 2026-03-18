package org.refit.refitbackend.global.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicsConfig {

    @Bean
    public NewTopic resumeParseRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getResumeParseRequested())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reportGenerateRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getReportGenerateRequested())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mentorEmbeddingRefreshRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getMentorEmbeddingRefreshRequested())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic resumeParseRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getResumeParseRequestedDlq())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reportGenerateRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getReportGenerateRequestedDlq())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mentorEmbeddingRefreshRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getMentorEmbeddingRefreshRequestedDlq())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatMessageSentTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getChatMessageSent())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getNotificationRequested())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getNotificationRequestedDlq())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationPushRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getNotificationPushRequested())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationPushRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getNotificationPushRequestedDlq())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatMessagePersistRequestedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getChatMessagePersistRequested())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic chatMessagePersistRequestedDlqTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.getChatMessagePersistRequestedDlq())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
