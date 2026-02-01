package org.refit.refitbackend.domain.master.repository;

import org.refit.refitbackend.domain.master.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}
