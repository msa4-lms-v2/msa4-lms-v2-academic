package com.msa4lmsv2academic.domain.transfer.response;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequest;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record DepartmentTransferResponseDTO(
        @Schema(description = "전과 신청 ID", example = "1") Long id,
        @Schema(description = "학생 ID", example = "1") Long studentId,
        @Schema(description = "학생명", example = "김학생") String studentName,
        @Schema(description = "신청 당시 학과 ID", example = "10") Long sourceDepartmentId,
        @Schema(description = "신청 당시 학과명", example = "컴퓨터공학과") String sourceDepartmentName,
        @Schema(description = "희망 학과 ID", example = "20") Long targetDepartmentId,
        @Schema(description = "희망 학과명", example = "경영학과") String targetDepartmentName,
        @Schema(description = "적용 희망 학기 ID", example = "23") Long targetSemesterId,
        @Schema(description = "적용 희망 학년도", example = "2027") short targetAcademicYear,
        @Schema(description = "적용 희망 학기", example = "FIRST") SemesterTerm targetTerm,
        @Schema(description = "처리 상태", example = "PENDING") AcademicChangeRequestStatus status,
        @Schema(description = "관리자 반려 사유", example = "필수 제출 서류의 내용이 확인되지 않습니다.") String rejectReason,
        @Schema(description = "처리 관리자 사용자 ID", example = "3") Long processedBy,
        @Schema(description = "처리 시각(KST)", example = "2027-02-10T10:00:00") LocalDateTime processedAt,
        @Schema(description = "학생 취소 사유", example = "진로 계획을 다시 검토하기로 했습니다.") String cancelReason,
        @Schema(description = "취소 사용자 ID", example = "1") Long cancelledBy,
        @Schema(description = "취소 시각(KST)", example = "2026-12-05T11:00:00") LocalDateTime cancelledAt,
        @Schema(description = "필수 PDF 2종 메타데이터. 저장 키는 노출하지 않습니다.")
        List<DepartmentTransferFileResponseDTO> documents,
        @Schema(description = "신청 시각(KST)", example = "2026-12-01T10:30:00") LocalDateTime createdAt,
        @Schema(description = "최종 변경 시각(KST)", example = "2026-12-01T10:30:00") LocalDateTime updatedAt
) {
    public static DepartmentTransferResponseDTO from(AcademicChangeRequest request) {
        return new DepartmentTransferResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
                request.getSourceDepartment().getId(),
                request.getSourceDepartment().getName(),
                request.getTargetDepartment().getId(),
                request.getTargetDepartment().getName(),
                request.getTargetSemester().getId(),
                request.getTargetSemester().getAcademicYear(),
                request.getTargetSemester().getTerm(),
                request.getStatus(),
                request.getRejectReason(),
                request.getProcessedBy() == null ? null : request.getProcessedBy().getId(),
                request.getProcessedAt(),
                request.getCancelReason(),
                request.getCancelledBy() == null ? null : request.getCancelledBy().getId(),
                request.getCancelledAt(),
                request.getFiles().stream().sorted(Comparator.comparing(f -> f.getDocumentType().name()))
                        .map(DepartmentTransferFileResponseDTO::from).toList(),
                request.getCreatedAt(), request.getUpdatedAt());
    }
}
