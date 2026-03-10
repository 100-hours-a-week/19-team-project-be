package org.refit.refitbackend.domain.chat.repository;

import org.refit.refitbackend.domain.chat.entity.ChatRoom;
import org.refit.refitbackend.domain.chat.entity.ChatRoomStatus;
import org.refit.refitbackend.domain.chat.repository.projection.ChatRoomReadStateProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.refit.refitbackend.domain.chat.repository.projection.ChatRoomListProjection;
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

    @Query("""
      SELECT
        cr.id AS chatId,
        cr.requester.id AS requesterId,
        cr.requester.nickname AS requesterNickname,
        cr.requester.profileImageUrl AS requesterProfileImageUrl,
        cr.requester.userType AS requesterUserType,
        cr.receiver.id AS receiverId,
        cr.receiver.nickname AS receiverNickname,
        cr.receiver.profileImageUrl AS receiverProfileImageUrl,
        cr.receiver.userType AS receiverUserType,
        lm.id AS lastMessageId,
        lm.roomSequence AS lastMessageRoomSequence,
        lm.content AS lastMessageContent,
        cr.lastMessageAt AS lastMessageAt,
        CASE
          WHEN cr.lastMessageSeq IS NULL THEN 0
          WHEN (cr.lastMessageSeq - (
            CASE
              WHEN cr.requester.id = :userId THEN COALESCE(cr.requesterLastReadSeq, 0)
              ELSE COALESCE(cr.receiverLastReadSeq, 0)
            END
          )) > 0 THEN (cr.lastMessageSeq - (
            CASE
              WHEN cr.requester.id = :userId THEN COALESCE(cr.requesterLastReadSeq, 0)
              ELSE COALESCE(cr.receiverLastReadSeq, 0)
            END
          ))
          ELSE 0
        END AS unreadCount,
        cr.status AS status,
        req.requestType AS requestType,
        cr.createdAt AS createdAt,
        cr.updatedAt AS updatedAt
      FROM ChatRoom cr
      LEFT JOIN cr.lastMessage lm
      LEFT JOIN ChatRequest req ON req.id = cr.chatRequestId
      WHERE (cr.requester.id = :userId OR cr.receiver.id = :userId)
        AND cr.status = :status
        AND (:cursorId IS NULL OR cr.id < :cursorId)
      ORDER BY cr.id DESC
      """)
    List<ChatRoomListProjection> findMyRoomSummariesByCursor(
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

    @Query("""
          SELECT COUNT(cr) > 0 FROM ChatRoom cr
          WHERE cr.id = :roomId
            AND (cr.requester.id = :userId OR cr.receiver.id = :userId)
      """)
    boolean existsByIdAndUserId(
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

    @Query("""
          SELECT COUNT(cr) > 0 FROM ChatRoom cr
          WHERE cr.resumeId = :resumeId
            AND (cr.requester.id = :userId OR cr.receiver.id = :userId)
      """)
    boolean existsByResumeIdAndUserId(
            @Param("resumeId") Long resumeId,
            @Param("userId") Long userId
    );

    @Query("""
          SELECT
            cr.id AS roomId,
            cr.requester.id AS requesterId,
            cr.receiver.id AS receiverId,
            cr.lastMessageSeq AS lastMessageSeq
          FROM ChatRoom cr
          WHERE cr.id = :roomId
            AND (cr.requester.id = :userId OR cr.receiver.id = :userId)
      """)
    Optional<ChatRoomReadStateProjection> findReadStateByIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
          UPDATE ChatRoom cr
          SET cr.requesterLastReadSeq = :lastReadSeq
          WHERE cr.id = :roomId
            AND cr.requester.id = :userId
            AND (cr.requesterLastReadSeq IS NULL OR cr.requesterLastReadSeq < :lastReadSeq)
      """)
    int updateRequesterLastReadSeqIfGreater(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("lastReadSeq") Long lastReadSeq
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
          UPDATE ChatRoom cr
          SET cr.receiverLastReadSeq = :lastReadSeq
          WHERE cr.id = :roomId
            AND cr.receiver.id = :userId
            AND (cr.receiverLastReadSeq IS NULL OR cr.receiverLastReadSeq < :lastReadSeq)
      """)
    int updateReceiverLastReadSeqIfGreater(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("lastReadSeq") Long lastReadSeq
    );
}
