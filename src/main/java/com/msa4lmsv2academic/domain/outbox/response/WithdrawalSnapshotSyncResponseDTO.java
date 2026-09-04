package com.msa4lmsv2academic.domain.outbox.response;

import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import java.time.LocalDate;

public record WithdrawalSnapshotSyncResponseDTO(
        Long withdrawalId,
        Long studentId,
        LocalDate effectiveDate,
        Long sourceVersion
) {
    public static WithdrawalSnapshotSyncResponseDTO from(WithdrawalRequest request) {
        return new WithdrawalSnapshotSyncResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getEffectiveDate(),
                request.getVersion()
        );
    }
}
