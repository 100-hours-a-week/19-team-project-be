package org.refit.refitbackend.domain.expert.repository;

import org.refit.refitbackend.domain.user.entity.User;
import org.refit.refitbackend.domain.user.entity.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<User, Long> {

    @Query("""
      SELECT DISTINCT u FROM User u
      LEFT JOIN u.userJobs uj
      LEFT JOIN u.userSkills us
      WHERE u.userType = 'EXPERT'
      AND (:keyword IS NULL
           OR u.nickname ILIKE CONCAT('%', CAST(:keyword AS text), '%')
           OR u.introduction ILIKE CONCAT('%', CAST(:keyword AS text), '%')
           OR uj.job.name ILIKE CONCAT('%', CAST(:keyword AS text), '%')
           OR us.skill.name ILIKE CONCAT('%', CAST(:keyword AS text), '%'))
      AND (:jobId IS NULL OR uj.job.id = :jobId)
      AND (:skillId IS NULL OR us.skill.id = :skillId)
      AND (:careerLevelId IS NULL OR u.careerLevel.id = :careerLevelId)
      AND (:cursorId IS NULL OR u.id < :cursorId)
      ORDER BY u.id DESC
  """)
    List<User> searchExpertsByCursor(
            @Param("keyword") String keyword,
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("careerLevelId") Long careerLevelId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

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

    @Query("""
      SELECT u FROM User u
      WHERE u.id = :userId
      AND u.userType = 'EXPERT'
  """)
    Optional<User> findExpertById(@Param("userId") Long userId);
}
