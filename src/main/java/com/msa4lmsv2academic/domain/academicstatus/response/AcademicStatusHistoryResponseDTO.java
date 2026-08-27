package com.msa4lmsv2academic.domain.academicstatus.response;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "확정된 학적 변경 이력. 학생 이름·학과는 현재 값이며 변경 당시 스냅샷이 아님")
public record AcademicStatusHistoryResponseDTO(
        @Schema(description = "학적 변경 이력 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Long historyId,
        @Schema(description = "Academic 학생 ID(Auth 사용자 ID와 다름)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long studentId,
        @Schema(description = "현재 학생 이름", example = "김학생", maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED)
        String studentName,
        @Schema(description = "현재 소속 학과 ID", example = "130", requiredMode = Schema.RequiredMode.REQUIRED)
        Long departmentId,
        @Schema(description = "현재 소속 학과명", example = "컴퓨터공학과", maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        String departmentName,
        @Schema(description = "변경 전 학적 상태", example = "ENROLLED", requiredMode = Schema.RequiredMode.REQUIRED)
        AcademicStatus previousStatus,
        @Schema(description = "변경 후 학적 상태", example = "WITHDRAWN", requiredMode = Schema.RequiredMode.REQUIRED)
        AcademicStatus newStatus,
        @Schema(description = "저장된 학적 변경 사유. 없으면 null", example = "자퇴 최종 승인", maxLength = 500,
                nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        String reason,
        @Schema(description = "처리자 users.id. 이름·역할·연락처는 제공하지 않음", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        Long changedBy,
        @Schema(description = "상태 전이 원인", example = "WITHDRAWAL_REQUEST", maxLength = 30,
                allowableValues = {"LEAVE_REQUEST", "WITHDRAWAL_REQUEST", "DISMISSAL", "ADMIN_CORRECTION", "READMISSION"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceType,
        @Schema(description = "원본 신청·조치 ID. 없으면 null. 원본 조회는 별도 API 권한 적용", example = "15",
                nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
        Long sourceId,
        @Schema(description = "이력 기록 시각(KST), 학적 적용일과 다를 수 있음", example = "2026-08-27T10:30:00",
                format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt
) {
    public static AcademicStatusHistoryResponseDTO from(AcademicStatusHistory history) {
        var student = history.getStudent();
        return new AcademicStatusHistoryResponseDTO(
                history.getId(), student.getId(), student.getUser().getName(), student.getDepartment().getId(),
                student.getDepartment().getName(), history.getPreviousStatus(), history.getNewStatus(),
                history.getReason(), history.getChangedBy().getId(), history.getSourceType(), history.getSourceId(),
                history.getCreatedAt()
        );
    }
}
