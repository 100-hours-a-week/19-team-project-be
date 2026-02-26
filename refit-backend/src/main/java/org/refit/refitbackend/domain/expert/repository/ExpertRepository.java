package org.refit.refitbackend.domain.expert.repository;

import org.refit.refitbackend.domain.expert.dto.ExpertSearchRow;
import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ExpertRepository extends JpaRepository<User, Long> {

    @Query(value = """
      SELECT u.id
      FROM users u
      WHERE u.user_type = 'EXPERT'
        AND u.status = 'ACTIVE'
        AND (:careerLevelId IS NULL OR u.career_level_id = :careerLevelId)
        AND (:cursorId IS NULL OR u.id < :cursorId)
        AND (:jobId IS NULL OR EXISTS (
             SELECT 1 FROM user_jobs uj2
             WHERE uj2.user_id = u.id AND uj2.job_id = :jobId
        ))
        AND (:skillId IS NULL OR EXISTS (
             SELECT 1 FROM user_skills us2
             WHERE us2.user_id = u.id AND us2.skill_id = :skillId
        ))
      ORDER BY u.id DESC
      LIMIT :limit
  """, nativeQuery = true)
    List<Long> searchExpertIdsByCursorNoKeyword(
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("careerLevelId") Long careerLevelId,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    @Query(value = """
      SELECT u.id
      FROM users u
      WHERE u.user_type = 'EXPERT'
        AND u.status = 'ACTIVE'
        AND (
             lower(u.nickname) LIKE :keywordPattern ESCAPE '\\' OR
             EXISTS (
                 SELECT 1
                 FROM user_jobs uj
                 JOIN jobs j ON j.id = uj.job_id
                 WHERE uj.user_id = u.id
                   AND lower(j.name) LIKE :keywordPattern ESCAPE '\\'
             ) OR
             EXISTS (
                 SELECT 1
                 FROM user_skills us
                 JOIN skills s ON s.id = us.skill_id
                 WHERE us.user_id = u.id
                   AND lower(s.name) LIKE :keywordPattern ESCAPE '\\'
             )
        )
        AND (:careerLevelId IS NULL OR u.career_level_id = :careerLevelId)
        AND (:cursorId IS NULL OR u.id < :cursorId)
        AND (:jobId IS NULL OR EXISTS (
             SELECT 1 FROM user_jobs uj2
             WHERE uj2.user_id = u.id AND uj2.job_id = :jobId
        ))
        AND (:skillId IS NULL OR EXISTS (
             SELECT 1 FROM user_skills us2
             WHERE us2.user_id = u.id AND us2.skill_id = :skillId
        ))
      ORDER BY u.id DESC
      LIMIT :limit
  """, nativeQuery = true)
    List<Long> searchExpertIdsByCursorWithKeyword(
            @Param("keywordPattern") String keywordPattern,
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("careerLevelId") Long careerLevelId,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    @Query(value = """
      SELECT EXISTS (
          SELECT 1
          FROM users u
          WHERE u.user_type = 'EXPERT'
            AND u.status = 'ACTIVE'
            AND lower(u.nickname) LIKE :keywordPattern ESCAPE '\\'
          LIMIT 1
      )
  """, nativeQuery = true)
    boolean existsNicknamePrefixMatch(@Param("keywordPattern") String keywordPattern);

    @Query(value = """
      SELECT EXISTS (
          SELECT 1
          FROM user_jobs uj
          JOIN users u ON u.id = uj.user_id
          JOIN jobs j ON j.id = uj.job_id
          WHERE u.user_type = 'EXPERT'
            AND u.status = 'ACTIVE'
            AND lower(j.name) LIKE :keywordPattern ESCAPE '\\'
          LIMIT 1
      )
  """, nativeQuery = true)
    boolean existsJobNamePrefixMatch(@Param("keywordPattern") String keywordPattern);

    @Query(value = """
      SELECT EXISTS (
          SELECT 1
          FROM user_skills us
          JOIN users u ON u.id = us.user_id
          JOIN skills s ON s.id = us.skill_id
          WHERE u.user_type = 'EXPERT'
            AND u.status = 'ACTIVE'
            AND lower(s.name) LIKE :keywordPattern ESCAPE '\\'
          LIMIT 1
      )
  """, nativeQuery = true)
    boolean existsSkillNamePrefixMatch(@Param("keywordPattern") String keywordPattern);

    @Query("""
      SELECT new org.refit.refitbackend.domain.expert.dto.ExpertSearchRow(
          u.id,
          u.nickname,
          u.profileImageUrl,
          u.introduction,
          cl.id,
          cl.level,
          ep.companyName,
          ep.verified,
          ep.ratingAvg,
          ep.ratingCount,
          ep.lastActiveAt
      )
      FROM User u
      LEFT JOIN u.expertProfile ep
      JOIN u.careerLevel cl
      WHERE u.id IN :userIds
      ORDER BY u.id DESC
  """)
    List<ExpertSearchRow> findExpertRowsByUserIds(@Param("userIds") List<Long> userIds);

      @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.expertProfile ep
        JOIN FETCH u.careerLevel cl
        WHERE u.id = :userId
          AND u.userType = 'EXPERT'
          AND u.status = 'ACTIVE'
    """)
      Optional<User> findExpertById(@Param("userId") Long userId);

      @Query("""
        SELECT u FROM User u
        JOIN FETCH u.expertProfile ep
        JOIN FETCH u.careerLevel cl
        WHERE u.userType = 'EXPERT'
          AND u.status = UserStatus.ACTIVE
          AND u.id <> :userId
          AND (:verified = false OR ep.verified = true)
        ORDER BY u.id DESC
    """)
      List<User> findRecommendationCandidates(
              @Param("userId") Long userId,
              @Param("verified") boolean verified
      );

      @Query("""
        SELECT u FROM User u
        JOIN FETCH u.expertProfile ep
        WHERE u.userType = 'EXPERT'
          AND u.status = UserStatus.ACTIVE
          AND (:verified = false OR ep.verified = true)
        ORDER BY ep.ratingCount DESC, ep.ratingAvg DESC, ep.lastActiveAt DESC, u.id DESC
    """)
      List<User> findTopPopularExperts(
              @Param("verified") boolean verified,
              Pageable pageable
      );

}
