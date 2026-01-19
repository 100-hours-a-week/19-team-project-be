package org.refit.refitbackend.domain.master.repository;

import org.refit.refitbackend.domain.master.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
