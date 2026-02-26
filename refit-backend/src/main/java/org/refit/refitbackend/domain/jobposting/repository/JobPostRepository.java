package org.refit.refitbackend.domain.jobposting.repository;

import org.refit.refitbackend.domain.jobposting.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    Optional<JobPost> findBySourceAndSourceJobId(String source, String sourceJobId);

    Optional<JobPost> findByUrlHash(String urlHash);
}
