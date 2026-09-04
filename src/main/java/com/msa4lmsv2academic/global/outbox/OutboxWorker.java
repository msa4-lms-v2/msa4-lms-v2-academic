package com.msa4lmsv2academic.global.outbox;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * spring.threads.virtual.enabled=true 환경에서 Spring의 {@code @Scheduled}(SimpleAsyncTaskScheduler)가
 * 배포 환경에서 아예 실행되지 않는 현상이 확인돼(로컬은 정상, 배포 pod는 몇 분간 단 한 번도 발행 시도가 없었음),
 * Spring 스케줄링 인프라를 거치지 않는 별도 ScheduledExecutorService로 직접 폴링한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxBatchProcessor batchProcessor;

    @Value("${academic.outbox.poll-interval-ms:2000}")
    private long pollIntervalMs;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "academic-outbox-worker");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::publishPending, pollIntervalMs, pollIntervalMs, TimeUnit.MILLISECONDS);
        log.info("OutboxWorker 폴링 시작 (interval={}ms)", pollIntervalMs);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // ScheduledExecutorService도 Runnable에서 예외가 새어나가면 이후 스케줄이 조용히 멈추므로
    // (ScheduledExecutorService 계약) 반드시 이 메서드 안에서 모든 예외를 잡는다.
    private void publishPending() {
        try {
            batchProcessor.publishPendingBatch();
        } catch (Exception exception) {
            log.error("outbox 폴링 중 예상치 못한 예외 발생", exception);
        }
    }
}
