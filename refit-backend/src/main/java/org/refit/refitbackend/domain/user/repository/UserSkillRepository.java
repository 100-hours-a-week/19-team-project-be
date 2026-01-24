package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Integer> {
    @Query("""
      SELECT us FROM UserSkill us
      JOIN FETCH us.skill
      WHERE us.user.id IN :userIds
  """)
    List<UserSkill> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
