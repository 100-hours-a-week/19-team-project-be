package org.refit.refitbackend.domain.user.repository;

import org.refit.refitbackend.domain.user.entity.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJobRepository extends JpaRepository<UserJob, Integer> {
}
