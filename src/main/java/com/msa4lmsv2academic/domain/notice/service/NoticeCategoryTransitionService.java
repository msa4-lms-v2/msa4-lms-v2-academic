package com.msa4lmsv2academic.domain.notice.service;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeCategoryTransitionService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeService noticeService;

    @Scheduled(cron = "${academic.notice.category-transition.cron:0 * * * * *}", zone = "Asia/Seoul")
    public void transitionExpiredImportantNotices() {
        noticeService.transitionExpiredImportantNotices(LocalDate.now(KOREA_ZONE));
    }
}
