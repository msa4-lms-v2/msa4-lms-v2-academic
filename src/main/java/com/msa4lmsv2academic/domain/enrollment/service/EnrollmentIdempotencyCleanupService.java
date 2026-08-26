package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentIdempotencyCleanupService {
    private final AcademicIdempotencyKeyRepository keyRepository;

    @Scheduled(cron = "${academic.enrollment.idempotency-cleanup.cron:0 * * * * *}")
    @Transactional
    public void removeExpiredCompletedKeys() {
        // 이 POST의 만료된 성공 응답만 정리합니다. 신청·이력 및 다른 endpoint 키는 보존합니다.
        keyRepository.deleteExpiredCompletedKeys(EnrollmentIdempotencyService.ENDPOINT, LocalDateTime.now());
    }
}
