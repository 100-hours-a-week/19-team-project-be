package org.refit.refitbackend.global.idempotency.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.idempotency.entity.ProcessedEvent;
import org.refit.refitbackend.global.idempotency.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean tryMarkProcessed(String consumerName, String eventKey) {
        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.of(consumerName, eventKey));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
