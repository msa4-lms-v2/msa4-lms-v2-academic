package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveIdempotencyCleanupService {
    private final AcademicIdempotencyKeyRepository repository;

    @Scheduled(cron = "${academic.leave.idempotency-cleanup.cron:0 * * * * *}")
    @Transactional
    public void removeExpiredCompletedKeys() {
        var now = LeaveRequestPolicy.now();
        repository.deleteExpiredCompletedKeys("POST /api/academic/leave-requests", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PATCH /api/academic/leave-requests/", now);
        repository.deleteExpiredCompletedKeys("POST /api/academic/leave-request-periods", now);
        repository.deleteExpiredCompletedKeysByEndpointPrefix("PUT /api/academic/leave-request-periods/", now);
    }
}
