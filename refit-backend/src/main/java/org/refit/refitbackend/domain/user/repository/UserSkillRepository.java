package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Integer> {
}
