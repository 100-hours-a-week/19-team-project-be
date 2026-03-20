package org.refit.refitbackend.domain.task.outbox.repository;

import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxMessage;
import org.refit.refitbackend.domain.task.outbox.entity.TaskOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskOutboxRepository extends JpaRepository<TaskOutboxMessage, Long> {

    @Query("""
            SELECT m
            FROM TaskOutboxMessage m
            WHERE m.status = :status
              AND m.nextAttemptAt <= :now
            ORDER BY m.id ASC
            """)
    List<TaskOutboxMessage> findPublishableBatch(
            @Param("status") TaskOutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
