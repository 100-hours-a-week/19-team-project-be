package org.refit.refitbackend.domain.task.kafka.event;

public record ResumeParseRequestedEvent(
        String taskId,
        Long userId,
        String fileUrl
) {
}
