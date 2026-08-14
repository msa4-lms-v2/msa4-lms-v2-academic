package com.msa4lmsv2academic.domain.withdrawal.response;

import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 환불률은 Payment가 실제 결제·환불 기록을 근거로 계산해 소유한다(수정사항.md 4.5) - Academic은
    // 승인 여부와 효력일만 내려주고 여기서 별도로 계산·중복 보유하지 않는다.
    public static WithdrawalResponseDTO from(WithdrawalRequest request) {
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
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
