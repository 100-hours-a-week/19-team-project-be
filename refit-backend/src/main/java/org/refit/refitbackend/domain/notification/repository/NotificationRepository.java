package org.refit.refitbackend.domain.notification.repository;

import org.refit.refitbackend.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Query("""
      SELECT n FROM Notification n
      WHERE n.user.id = :userId
      AND (:cursorId IS NULL OR n.id < :cursorId)
      ORDER BY CASE WHEN n.isRead = false THEN 0 ELSE 1 END ASC, n.id DESC
    """)
    List<Notification> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      UPDATE Notification n
      SET n.isRead = true, n.readAt = :readAt
      WHERE n.user.id = :userId
      AND n.isRead = false
    """)
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
