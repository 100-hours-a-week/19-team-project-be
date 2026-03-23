package org.refit.refitbackend.global.idempotency.repository;

import org.refit.refitbackend.global.idempotency.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO processed_events (consumer_name, event_key, created_at, updated_at)
            VALUES (:consumerName, :eventKey, now(), now())
            ON CONFLICT (consumer_name, event_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnore(
            @Param("consumerName") String consumerName,
            @Param("eventKey") String eventKey
    );
}
