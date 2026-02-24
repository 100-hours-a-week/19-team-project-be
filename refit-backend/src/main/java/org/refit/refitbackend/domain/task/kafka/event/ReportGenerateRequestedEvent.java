package org.refit.refitbackend.domain.task.kafka.event;

public record ReportGenerateRequestedEvent(
        String taskId,
        Long userId,
        Long reportId,
        Long chatRoomId
) {
}
