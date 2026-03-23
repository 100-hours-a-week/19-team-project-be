package org.refit.refitbackend.domain.notification.outbox.repository;

import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxMessage;
import org.refit.refitbackend.domain.notification.outbox.entity.NotificationOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxMessage, Long> {

    @Query("""
            SELECT m
            FROM NotificationOutboxMessage m
            WHERE m.status = :status
              AND m.nextAttemptAt <= :now
            ORDER BY m.id ASC
            """)
    List<NotificationOutboxMessage> findPublishableBatch(
            @Param("status") NotificationOutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
