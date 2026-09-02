package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WithdrawalIdempotencyCleanupService {
    private final AcademicIdempotencyKeyRepository keyRepository;

    @Scheduled(cron = "${academic.withdrawal.idempotency-cleanup.cron:0 * * * * *}")
    @Transactional
    public void removeExpiredCompletedKeys() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        // 자퇴 API의 만료된 성공 응답만 삭제합니다. 신청/감사 및 수강신청 키는 보존합니다.
        keyRepository.deleteExpiredCompletedKeys("POST /api/academic/withdrawals", now);
        keyRepository.deleteExpiredCompletedKeysByEndpointPrefix("PATCH /api/academic/withdrawals/", now);
    }
}
