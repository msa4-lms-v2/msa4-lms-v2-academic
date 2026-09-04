package com.msa4lmsv2academic.global.outbox;

import com.msa4lmsv2academic.domain.outbox.entity.OutboxEvent;
import com.msa4lmsv2academic.domain.outbox.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private static final int BATCH_SIZE = 20;
    private static final int LEASE_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;
    private static final long[] BACKOFF_SECONDS = {2, 4, 8, 16, 32};
    private static final String WORKER_ID = "academic-outbox-worker";

    private static final Map<String, String> EVENT_TYPE_TO_TOPIC = Map.of(
            "StudentSnapshotChanged", "msa4-team3.academic.student-changed",
            "SemesterCreated", "msa4-team3.academic.semester-created",
            "WithdrawalApproved", "msa4-team3.academic.withdrawal-approved"
    );

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${academic.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(now, BATCH_SIZE);
        for (OutboxEvent event : batch) {
            event.lock(WORKER_ID, now.plusSeconds(LEASE_SECONDS));
            publish(event, now);
        }
    }

    private void publish(OutboxEvent event, LocalDateTime now) {
        String topic = EVENT_TYPE_TO_TOPIC.get(event.getEventType());
        if (topic == null) {
            event.giveUp("UNKNOWN_EVENT_TYPE");
            log.warn("알 수 없는 outbox event_type={} (id={})", event.getEventType(), event.getId());
            return;
        }
        String value;
        try {
            value = objectMapper.writeValueAsString(event.getPayload());
        } catch (JacksonException exception) {
            event.giveUp("PAYLOAD_SERIALIZATION_FAILED");
            log.error("outbox payload 직렬화 실패 (id={})", event.getId(), exception);
            return;
        }
        try {
            kafkaTemplate.send(topic, String.valueOf(event.getAggregateId()), value).get();
            event.complete(now);
        } catch (Exception exception) {
            retryOrGiveUp(event, now, exception);
        }
    }

    private void retryOrGiveUp(OutboxEvent event, LocalDateTime now, Exception exception) {
        if (event.getAttempts() + 1 >= MAX_ATTEMPTS) {
            event.giveUp("KAFKA_PUBLISH_FAILED");
            log.error("outbox 발행 {}회 실패, 수동 개입 필요 (id={})", MAX_ATTEMPTS, event.getId(), exception);
            return;
        }
        long backoff = BACKOFF_SECONDS[Math.min(event.getAttempts(), BACKOFF_SECONDS.length - 1)];
        event.retryLater(now.plusSeconds(backoff), "KAFKA_PUBLISH_FAILED");
        log.warn("outbox 발행 실패, {}초 후 재시도 (id={}, attempts={})", backoff, event.getId(), event.getAttempts(), exception);
    }
}
