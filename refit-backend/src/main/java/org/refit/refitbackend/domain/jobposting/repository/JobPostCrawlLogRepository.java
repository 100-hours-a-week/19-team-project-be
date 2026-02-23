package org.refit.refitbackend.domain.jobposting.repository;

import org.refit.refitbackend.domain.jobposting.entity.JobPostCrawlLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostCrawlLogRepository extends JpaRepository<JobPostCrawlLog, Long> {
}
