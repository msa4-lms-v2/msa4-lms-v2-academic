package com.msa4lmsv2academic.domain.withdrawal.response;

import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "자퇴 신청 처리 결과. ID와 학생 ID는 각 업무 PK이며 사용자 ID와 다릅니다. 환불률은 Payment 소유입니다.")
public record WithdrawalResponseDTO(
        @Schema(description = "자퇴 신청 ID", minimum = "1", example = "1") Long id,
        @Schema(description = "Academic 학생 ID", minimum = "1", example = "41") Long studentId,
        @Schema(description = "학생 이름", example = "김학생") String studentName,
        @Schema(description = "원래 신청 사유", maxLength = 500, example = "개인 사정") String reason,
        @Schema(description = "학생 희망일. 자동 승인되지 않음", nullable = true, format = "date", example = "2026-09-01")
        LocalDate requestedEffectiveDate,
        @Schema(description = "관리자 승인 시 확정된 적용일. 신규 승인은 승인 당일(KST)", nullable = true,
                format = "date", example = "2026-09-02") LocalDate effectiveDate,
        @Schema(description = "처리 상태. CANCELLED는 학생 취소이며 학적 변경 없음", example = "PENDING") WithdrawalStatus status,
        @Schema(description = "검토 교수의 Academic 사용자 ID", nullable = true, minimum = "1", example = "11") Long advisorReviewedBy,
        @Schema(description = "지도교수 검토 시각(KST)", nullable = true, format = "date-time", example = "2026-09-01T10:00:00")
        LocalDateTime advisorReviewedAt,
        @Schema(description = "지도교수 반려 사유", nullable = true, maxLength = 500, example = "상담 필요") String advisorRejectReason,
        @Schema(description = "최종 처리 관리자의 Academic 사용자 ID", nullable = true, minimum = "1", example = "3") Long processedBy,
        @Schema(description = "최종 처리 시각(KST)", nullable = true, format = "date-time", example = "2026-09-02T10:00:00")
        LocalDateTime processedAt,
        @Schema(description = "관리자 반려 사유", nullable = true, maxLength = 500, example = "추가 확인 필요") String rejectReason,
        @Schema(description = "신청 생성 시각(KST)", format = "date-time", example = "2026-09-01T09:00:00") LocalDateTime createdAt,
        @Schema(description = "최종 변경 시각(KST)", format = "date-time", example = "2026-09-02T10:00:00") LocalDateTime updatedAt,
        @Schema(description = "취소 사유. 원래 신청 사유를 덮어쓰지 않음", nullable = true, maxLength = 255,
                example = "학업을 계속하기로 결정했습니다.") String cancelReason,
        @Schema(description = "취소한 학생의 Academic 사용자 ID", nullable = true, minimum = "1", example = "21") Long cancelledBy,
        @Schema(description = "취소 시각(KST)", nullable = true, format = "date-time", example = "2026-09-01T11:00:00")
        LocalDateTime cancelledAt,
        @Schema(description = "현재 연결된 PDF 원본 파일명. MinIO 저장 키는 응답하지 않음", nullable = true,
                maxLength = 255, example = "자퇴증빙.pdf") String attachmentOriginalName,
        @Schema(description = "현재 연결된 증빙 MIME 타입", nullable = true, maxLength = 100,
                example = "application/pdf") String attachmentContentType,
        @Schema(description = "현재 연결된 증빙 크기(byte), 최대 10485760", nullable = true,
                maximum = "10485760", example = "1024") Long attachmentSize
) {
    public static WithdrawalResponseDTO from(WithdrawalRequest request) {
        return new WithdrawalResponseDTO(
                request.getId(), request.getStudent().getId(), request.getStudent().getUser().getName(),
                request.getReason(), request.getRequestedEffectiveDate(), request.getEffectiveDate(), request.getStatus(),
                request.getAdvisorReviewedBy() == null ? null : request.getAdvisorReviewedBy().getId(),
                request.getAdvisorReviewedAt(), request.getAdvisorRejectReason(),
                request.getProcessedBy() == null ? null : request.getProcessedBy().getId(),
                request.getProcessedAt(), request.getRejectReason(), request.getCreatedAt(), request.getUpdatedAt(),
                request.getCancelReason(), request.getCancelledBy() == null ? null : request.getCancelledBy().getId(),
                request.getCancelledAt(), request.getAttachmentOriginalName(), request.getAttachmentContentType(),
                request.getAttachmentSize());
    }
}
