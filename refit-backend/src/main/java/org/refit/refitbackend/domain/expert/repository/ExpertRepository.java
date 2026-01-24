package org.refit.refitbackend.domain.expert.repository;

import org.refit.refitbackend.domain.user.entity.User;
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
           OR u.nickname ILIKE CONCAT('%', :keyword, '%')
           OR u.introduction ILIKE CONCAT('%', :keyword, '%')
           OR uj.job.name ILIKE CONCAT('%', :keyword, '%')
           OR us.skill.name ILIKE CONCAT('%', :keyword, '%'))
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
      WHERE u.id = :userId
      AND u.userType = 'EXPERT'
  """)
    Optional<User> findExpertById(@Param("userId") Long userId);
}
