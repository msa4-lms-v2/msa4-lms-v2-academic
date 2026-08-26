package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "수강신청 성공 결과. 보존 기간 내 동일 요청은 저장된 결과를 재생합니다.")
public record StudentEnrollmentCreateResponseDTO(
        @Schema(description = "새로 생성된 수강 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED) Long enrollmentId,
        @Schema(description = "본인 학생 ID (users.id와 구분)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long studentId,
        @Schema(description = "개설 강의 ID", example = "20", requiredMode = Schema.RequiredMode.REQUIRED) Long lectureId,
        @Schema(description = "신청 시점 수강 상태", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED) EnrollmentStatus status,
        @Schema(description = "신청 시각 (서버 시간)", example = "2026-08-26T11:00:00",
                type = "string", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime enrolledAt
) {
    public static StudentEnrollmentCreateResponseDTO from(Enrollment enrollment) {
        return new StudentEnrollmentCreateResponseDTO(enrollment.getId(), enrollment.getStudent().getId(),
                enrollment.getLecture().getId(), enrollment.getStatus(), enrollment.getEnrolledAt());
    }
}
