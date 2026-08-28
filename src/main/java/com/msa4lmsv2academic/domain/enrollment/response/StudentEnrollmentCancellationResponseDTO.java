package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "학생 본인 수강 취소 결과")
public record StudentEnrollmentCancellationResponseDTO(
        @Schema(description = "취소한 수강신청 ID", example = "101") Long enrollmentId,
        @Schema(description = "본인 학생 ID (users.id와 구분)", example = "1") Long studentId,
        @Schema(description = "개설 강의 ID", example = "20") Long lectureId,
        @Schema(description = "취소 후 수강 상태", example = "CANCELLED") EnrollmentStatus status,
        @Schema(description = "취소 처리 시각", example = "2026-08-28T10:30:00") LocalDateTime cancelledAt
) {

    public static StudentEnrollmentCancellationResponseDTO from(Enrollment enrollment, LocalDateTime cancelledAt) {
        return new StudentEnrollmentCancellationResponseDTO(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getLecture().getId(),
                enrollment.getStatus(),
                cancelledAt
        );
    }
}
