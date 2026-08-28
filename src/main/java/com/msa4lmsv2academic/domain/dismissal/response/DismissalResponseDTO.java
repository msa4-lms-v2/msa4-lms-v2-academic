package com.msa4lmsv2academic.domain.dismissal.response;

import com.msa4lmsv2academic.domain.dismissal.entity.*;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "ADMIN 전용 제적 후보. 학생 인적사항과 학적은 조회 시점 값이며 성공 응답 재생은 저장 시점 값")
public record DismissalResponseDTO(
        @Schema(description = "제적 후보 ID", example = "1") Long id,
        @Schema(description = "현재 버전. 수정·확정·취소 요청에 그대로 전달", example = "0") long version,
        @Schema(description = "학생 ID", example = "1") Long studentId,
        @Schema(description = "학생 이름", example = "김학생", maxLength = 50) String studentName,
        @Schema(description = "현재 학과 ID", example = "130") Long departmentId,
        @Schema(description = "현재 학적", example = "ENROLLED") AcademicStatus academicStatus,
        @Schema(description = "제적 종류", example = "DISCIPLINARY") DismissalReasonType reasonType,
        @Schema(description = "관리자용 상세 근거", example = "징계 심의 결과 확인", maxLength = 500) String reason,
        @Schema(description = "후보 처리 상태", example = "PENDING") DismissalStatus status,
        @Schema(description = "최초 등록자 users.id", example = "3") Long registeredBy,
        @Schema(description = "확정/취소 처리자 users.id, 대기 시 null", example = "3", nullable = true) Long processedBy,
        @Schema(description = "확정/취소 시각(KST), 대기 시 null", example = "2026-08-28T14:00:00", nullable = true) LocalDateTime processedAt,
        @Schema(description = "취소 사유, 대기/확정 시 null", maxLength = 500, nullable = true) String cancelReason,
        @Schema(description = "등록 시각(KST)", example = "2026-08-28T13:00:00") LocalDateTime createdAt,
        @Schema(description = "마지막 변경 시각(KST)", example = "2026-08-28T14:00:00") LocalDateTime updatedAt
) {
    public static DismissalResponseDTO from(DismissalCandidate candidate) {
        var student = candidate.getStudent();
        return new DismissalResponseDTO(candidate.getId(), candidate.getVersion(), student.getId(),
                student.getUser().getName(), student.getDepartment().getId(), student.getAcademicStatus(),
                candidate.getReasonType(), candidate.getReason(), candidate.getStatus(), candidate.getRegisteredBy(),
                candidate.getProcessedBy(), candidate.getProcessedAt(), candidate.getCancelReason(),
                candidate.getCreatedAt(), candidate.getUpdatedAt());
    }
}
