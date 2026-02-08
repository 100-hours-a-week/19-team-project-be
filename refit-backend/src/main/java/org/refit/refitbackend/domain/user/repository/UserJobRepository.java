package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
 public interface UserJobRepository extends JpaRepository<UserJob, Long> {
    void deleteByUser_Id(Long userId);

    @Query("""
      SELECT uj FROM UserJob uj
      JOIN FETCH uj.job
      WHERE uj.user.id IN :userIds
  """)
    List<UserJob> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
