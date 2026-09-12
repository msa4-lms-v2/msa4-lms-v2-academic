package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import com.msa4lmsv2academic.global.scheduling.CronScheduling;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * spring.threads.virtual.enabled=true 환경에서 Spring {@code @Scheduled}가 배포 pod에서 실행되지
 * 않는 현상이 확인돼(OutboxWorker 참고), 별도 ScheduledExecutorService로 직접 폴링한다.
 * removeExpiredCompletedKeys()의 @Transactional은 AOP 프록시를 거쳐야 적용되므로, this로 직접
 * 호출하지 않고 지연 주입한 자기 자신(self)의 프록시를 통해 호출한다(self-invocation 우회).
 */
@Slf4j
@Service
public class LeaveIdempotencyCleanupService {
    private final AcademicIdempotencyKeyRepository repository;
    private final LeaveIdempotencyCleanupService self;

    @Value("${academic.leave.idempotency-cleanup.cron:0 * * * * *}")
    private String cron;

    private ScheduledExecutorService scheduler;

    public LeaveIdempotencyCleanupService(AcademicIdempotencyKeyRepository repository,
                                           @Lazy LeaveIdempotencyCleanupService self) {
        this.repository = repository;
        this.self = self;
    }

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "leave-idempotency-cleanup-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        CronScheduling.scheduleCron(scheduler, cron, ZoneId.systemDefault(), this::runRemoveExpiredCompletedKeys);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runRemoveExpiredCompletedKeys() {
        try {
            self.removeExpiredCompletedKeys();
        } catch (Exception exception) {
            log.error("휴학 처리 멱등키 정리 중 예상치 못한 예외 발생", exception);
        }
    }

    @Transactional
    public void removeExpiredCompletedKeys() {
        var now = LeaveRequestPolicy.now();
        repository.deleteExpiredCompletedKeys("POST /api/academic/leave-requests", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PATCH /api/academic/leave-requests/", now);
        repository.deleteExpiredCompletedKeys("POST /api/academic/leave-request-periods", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PUT /api/academic/leave-request-periods/", now);
    }
}
