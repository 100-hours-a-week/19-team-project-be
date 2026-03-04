package org.refit.refitbackend.domain.task.kafka.event;

public record MentorEmbeddingRefreshRequestedEvent(
        String taskId,
        Long userId
) {
}
