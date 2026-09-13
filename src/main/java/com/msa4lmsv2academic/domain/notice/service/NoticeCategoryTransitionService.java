package com.msa4lmsv2academic.domain.notice.service;

import com.msa4lmsv2academic.global.scheduling.CronScheduling;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * spring.threads.virtual.enabled=true 환경에서 Spring {@code @Scheduled}가 배포 pod에서 실행되지
 * 않는 현상이 확인돼(OutboxWorker 참고), 별도 ScheduledExecutorService로 직접 폴링한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeCategoryTransitionService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeService noticeService;

    @Value("${academic.notice.category-transition.cron:0 * * * * *}")
    private String cron;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notice-category-transition-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        CronScheduling.scheduleCron(scheduler, cron, KOREA_ZONE, this::runTransitionExpiredImportantNotices);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void runTransitionExpiredImportantNotices() {
        try {
            noticeService.transitionExpiredImportantNotices(LocalDate.now(KOREA_ZONE));
        } catch (Exception exception) {
            log.error("만료된 중요 공지 카테고리 전환 중 예상치 못한 예외 발생", exception);
        }
    }
}
