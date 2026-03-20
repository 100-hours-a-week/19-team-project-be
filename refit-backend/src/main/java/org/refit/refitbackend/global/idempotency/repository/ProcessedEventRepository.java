package org.refit.refitbackend.global.idempotency.repository;

import org.refit.refitbackend.global.idempotency.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
}
