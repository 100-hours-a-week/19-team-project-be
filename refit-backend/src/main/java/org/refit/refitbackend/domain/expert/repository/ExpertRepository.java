package org.refit.refitbackend.domain.expert.repository;

import org.refit.refitbackend.domain.expert.dto.ExpertSearchRow;
import org.refit.refitbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

@Repository
public interface ExpertRepository extends JpaRepository<User, Long> {

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
      WHERE u.userType = 'EXPERT'
        AND u.status = 'ACTIVE'
        AND (:jobId IS NULL OR EXISTS (
             SELECT 1 FROM UserJob uj2
             WHERE uj2.user = u AND uj2.job.id = :jobId
        ))
        AND (:skillId IS NULL OR EXISTS (
             SELECT 1 FROM UserSkill us2
             WHERE us2.user = u AND us2.skill.id = :skillId
        ))
        AND (:careerLevelId IS NULL OR u.careerLevel.id = :careerLevelId)
        AND (:cursorId IS NULL OR u.id < :cursorId)
      ORDER BY u.id DESC
  """)
    List<ExpertSearchRow> searchExpertsByCursorNoKeyword(
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("careerLevelId") Long careerLevelId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

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
      WHERE u.userType = 'EXPERT'
        AND u.status = 'ACTIVE'
        AND (
             lower(u.nickname) LIKE :keywordPattern ESCAPE '\\' OR
             lower(u.introduction) LIKE :keywordPattern ESCAPE '\\' OR
             EXISTS (
                 SELECT 1 FROM UserJob uj
                 JOIN uj.job j
                 WHERE uj.user = u
                   AND lower(j.name) LIKE :keywordPattern ESCAPE '\\'
             ) OR
             EXISTS (
                 SELECT 1 FROM UserSkill us
                 JOIN us.skill s
                 WHERE us.user = u
                   AND lower(s.name) LIKE :keywordPattern ESCAPE '\\'
             )
        )
        AND (:jobId IS NULL OR EXISTS (
             SELECT 1 FROM UserJob uj2
             WHERE uj2.user = u AND uj2.job.id = :jobId
        ))
        AND (:skillId IS NULL OR EXISTS (
             SELECT 1 FROM UserSkill us2
             WHERE us2.user = u AND us2.skill.id = :skillId
        ))
        AND (:careerLevelId IS NULL OR u.careerLevel.id = :careerLevelId)
        AND (:cursorId IS NULL OR u.id < :cursorId)
      ORDER BY u.id DESC
  """)
    List<ExpertSearchRow> searchExpertsByCursorWithKeyword(
            @Param("keywordPattern") String keywordPattern,
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("careerLevelId") Long careerLevelId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
      SELECT u FROM User u
      LEFT JOIN FETCH u.expertProfile ep
      JOIN FETCH u.careerLevel cl
      WHERE u.id = :userId
        AND u.userType = 'EXPERT'
        AND u.status = 'ACTIVE'
  """)
    Optional<User> findExpertById(@Param("userId") Long userId);
}
