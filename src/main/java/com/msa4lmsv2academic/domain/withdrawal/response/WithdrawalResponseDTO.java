package com.msa4lmsv2academic.domain.withdrawal.response;

import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WithdrawalResponseDTO(
        Long id,
        Long studentId,
        String studentName,
        String reason,
        LocalDate requestedEffectiveDate,
        LocalDate effectiveDate,
        WithdrawalStatus status,
        Long advisorReviewedBy,
        LocalDateTime advisorReviewedAt,
        String advisorRejectReason,
        Long processedBy,
        LocalDateTime processedAt,
        String rejectReason,
        BigDecimal refundRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WithdrawalResponseDTO from(WithdrawalRequest request, BigDecimal refundRate) {
        return new WithdrawalResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
                request.getReason(),
                request.getRequestedEffectiveDate(),
                request.getEffectiveDate(),
                request.getStatus(),
                request.getAdvisorReviewedBy() == null ? null : request.getAdvisorReviewedBy().getId(),
                request.getAdvisorReviewedAt(),
                request.getAdvisorRejectReason(),
                request.getProcessedBy() == null ? null : request.getProcessedBy().getId(),
                request.getProcessedAt(),
                request.getRejectReason(),
                refundRate,
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
