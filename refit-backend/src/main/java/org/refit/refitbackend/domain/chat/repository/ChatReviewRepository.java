package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatReviewRepository extends JpaRepository<ChatReview, Long> {

    boolean existsByChatRoomId(Long chatRoomId);

    Optional<ChatReview> findByChatRoomId(Long chatRoomId);

    Optional<ChatReview> findByChatRoomIdAndReviewerId(Long chatRoomId, Long reviewerId);

    @Query("""
        SELECT cr FROM ChatReview cr
        JOIN FETCH cr.reviewer r
        WHERE cr.reviewee.id = :expertId
          AND (:cursorId IS NULL OR cr.id < :cursorId)
        ORDER BY cr.id DESC
        """)
    List<ChatReview> findByRevieweeIdWithReviewerByCursor(
            @Param("expertId") Long expertId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
