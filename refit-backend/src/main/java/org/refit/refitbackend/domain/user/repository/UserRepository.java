package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.OAuthProvider;
import org.refit.refitbackend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider oauthProvider, String oauthId);

    boolean existsByOauthProviderAndOauthId(OAuthProvider oauthProvider, String oauthId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("""
      SELECT DISTINCT u FROM User u
      LEFT JOIN u.userJobs uj
      LEFT JOIN u.userSkills us
      WHERE (:keyword IS NULL
           OR u.nickname ILIKE CONCAT('%', CAST(:keyword AS text), '%')
           OR uj.job.name ILIKE CONCAT('%', CAST(:keyword AS text), '%')
           OR us.skill.name ILIKE CONCAT('%', CAST(:keyword AS text), '%'))
      AND (:jobId IS NULL OR uj.job.id = :jobId)
      AND (:skillId IS NULL OR us.skill.id = :skillId)
      AND (:cursorId IS NULL OR u.id < :cursorId)
      ORDER BY u.id DESC
  """)
    java.util.List<User> searchUsersByCursor(
            @Param("keyword") String keyword,
            @Param("jobId") Long jobId,
            @Param("skillId") Long skillId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
