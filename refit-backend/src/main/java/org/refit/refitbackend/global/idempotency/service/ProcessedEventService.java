package org.refit.refitbackend.global.idempotency.service;

import lombok.RequiredArgsConstructor;
import org.refit.refitbackend.global.idempotency.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean tryMarkProcessed(String consumerName, String eventKey) {
        return processedEventRepository.insertIgnore(consumerName, eventKey) > 0;
    }
}
