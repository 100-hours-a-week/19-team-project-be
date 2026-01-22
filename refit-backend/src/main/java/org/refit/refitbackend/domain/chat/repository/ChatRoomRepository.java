package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // (CURSOR) 내 채팅방 목록 조회 (요청자 또는 수신자로 참여)
    @Query("""
  SELECT cr FROM ChatRoom cr
  WHERE (cr.requester.id = :userId OR cr.receiver.id = :userId)
    AND cr.status = :status
    AND (:cursorId IS NULL OR cr.id < :cursorId)
  ORDER BY cr.id DESC
  """)
    List<ChatRoom> findMyRoomsByCursor(
            @Param("userId") Long userId,
            @Param("status") ChatRoomStatus status,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 특정 채팅방 조회 (권한 체크용)
    @Query("""                                                                                                                                                                   
          SELECT cr FROM ChatRoom cr                                                                                                                                               
          WHERE cr.id = :roomId                                                                                                                                                    
          AND (cr.requester.id = :userId OR cr.receiver.id = :userId)                                                                                                              
      """)
    Optional<ChatRoom> findByIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    // 두 사용자 간 활성 채팅방 존재 여부 확인
    @Query("""                                                                                                                                                                   
          SELECT cr FROM ChatRoom cr                                                                                                                                               
          WHERE ((cr.requester.id = :userId1 AND cr.receiver.id = :userId2)                                                                                                        
              OR (cr.requester.id = :userId2 AND cr.receiver.id = :userId1))                                                                                                       
          AND cr.status = 'ACTIVE'                                                                                                                                                 
      """)
    Optional<ChatRoom> findActiveRoomBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );
}
