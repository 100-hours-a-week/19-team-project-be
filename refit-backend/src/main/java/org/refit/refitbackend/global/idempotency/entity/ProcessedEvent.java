package org.refit.refitbackend.global.idempotency.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.refit.refitbackend.global.common.entity.BaseEntity;

@Entity
@Table(name = "processed_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_events_consumer_event", columnNames = {"consumer_name", "event_key"})
}, indexes = {
        @Index(name = "idx_processed_events_consumer_created", columnList = "consumer_name, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "event_key", nullable = false, length = 150)
    private String eventKey;

    @Builder
    private ProcessedEvent(String consumerName, String eventKey) {
        this.consumerName = consumerName;
        this.eventKey = eventKey;
    }

    public static ProcessedEvent of(String consumerName, String eventKey) {
        return ProcessedEvent.builder()
                .consumerName(consumerName)
                .eventKey(eventKey)
                .build();
    }
}
