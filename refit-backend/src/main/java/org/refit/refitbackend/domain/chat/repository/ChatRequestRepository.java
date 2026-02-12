package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatRequest;
import org.refit.refitbackend.domain.chat.entity.ChatRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {

    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId, ChatRequestStatus status);

    @Query("""
        SELECT cr FROM ChatRequest cr
        WHERE cr.receiver.id = :userId
          AND (:status IS NULL OR cr.status = :status)
          AND (:cursorId IS NULL OR cr.id < :cursorId)
        ORDER BY cr.id DESC
        """)
    List<ChatRequest> findReceivedByCursor(
            @Param("userId") Long userId,
            @Param("status") ChatRequestStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        SELECT cr FROM ChatRequest cr
        WHERE cr.requester.id = :userId
          AND (:status IS NULL OR cr.status = :status)
          AND (:cursorId IS NULL OR cr.id < :cursorId)
        ORDER BY cr.id DESC
        """)
    List<ChatRequest> findSentByCursor(
            @Param("userId") Long userId,
            @Param("status") ChatRequestStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
