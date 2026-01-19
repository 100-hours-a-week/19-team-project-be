package org.refit.refitbackend.domain.master.repository;

import org.refit.refitbackend.domain.master.entity.CareerLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerLevelRepository extends JpaRepository<CareerLevel, Long> {
}
