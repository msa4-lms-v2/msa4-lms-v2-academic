package com.msa4lmsv2academic.global.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxBatchProcessor batchProcessor;

    // spring.threads.virtual.enabled=true 환경에서는 SimpleAsyncTaskScheduler가 쓰이는데,
    // 이 스케줄러는 fixedDelay 작업에서 예외가 한 번이라도 새어나가면 이후 재스케줄을 멈춘다
    // (https://github.com/spring-projects/spring-boot/issues/38846). 어떤 예외도 이 메서드
    // 밖으로 나가지 않도록 최상위에서 전부 잡는다. batchProcessor는 별도 빈이라 @Transactional이
    // 프록시를 통해 정상 적용된다(같은 빈 안에서 self-invocation하면 프록시를 우회해 적용 안 됨).
    @Scheduled(fixedDelayString = "${academic.outbox.poll-interval-ms:2000}")
    public void publishPending() {
        try {
            batchProcessor.publishPendingBatch();
        } catch (Exception exception) {
            log.error("outbox 폴링 중 예상치 못한 예외 발생", exception);
        }
    }
}
