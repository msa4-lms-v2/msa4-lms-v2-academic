package com.msa4lmsv2academic.domain.outbox.service;

import com.msa4lmsv2academic.domain.outbox.entity.OutboxEvent;
import com.msa4lmsv2academic.domain.outbox.repository.OutboxEventRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String aggregateType, Long aggregateId, String eventType,
                       Map<String, Object> payload, Long sourceVersion) {
        outboxEventRepository.save(OutboxEvent.create(aggregateType, aggregateId, eventType, payload, sourceVersion));
    }
}
