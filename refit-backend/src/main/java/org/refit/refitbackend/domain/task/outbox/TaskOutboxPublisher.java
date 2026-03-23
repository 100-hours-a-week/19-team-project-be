package org.refit.refitbackend.domain.task.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxMessage;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxStatus;
import org.refit.refitbackend.domain.task.outbox.repository.TaskOutboxRepository;
import org.refit.refitbackend.domain.task.outbox.service.TaskOutboxPublishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.kafka.enabled", "app.task.outbox.enabled"}, havingValue = "true")
public class TaskOutboxPublisher {

    private final TaskOutboxRepository taskOutboxRepository;
    private final TaskOutboxPublishService taskOutboxPublishService;

    @Value("${app.task.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.task.outbox.publish-fixed-delay-ms:1000}")
    public void publishPendingMessages() {
        List<TaskOutboxMessage> batch = taskOutboxRepository.findPublishableBatch(
                TaskOutboxStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (TaskOutboxMessage message : batch) {
            taskOutboxPublishService.publish(message.getId());
        }
    }
}
