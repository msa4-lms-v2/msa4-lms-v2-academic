package com.msa4lmsv2academic.domain.dismissal.service;

import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DismissalIdempotencyCleanupService {
    private final AcademicIdempotencyKeyRepository repository;

    @Scheduled(cron = "${academic.dismissal.idempotency-cleanup.cron:0 * * * * *}")
    @Transactional
    public void cleanExpired() {
        var now = DismissalPolicy.now();
        repository.deleteExpiredCompletedKeys("POST /api/academic/dismissals", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PUT /api/academic/dismissals/", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PATCH /api/academic/dismissals/", now);
    }
}
