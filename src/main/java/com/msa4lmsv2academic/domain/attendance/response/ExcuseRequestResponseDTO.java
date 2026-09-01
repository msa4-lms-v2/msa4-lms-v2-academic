package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "공결 신청 처리 결과")
public record ExcuseRequestResponseDTO(
        @Schema(description = "공결 신청 ID", example = "301")
        Long id,

        @Schema(description = "수강신청 ID", example = "12001")
        Long enrollmentId,

        @Schema(description = "공결 대상 수업일", example = "2026-09-01")
        LocalDate lectureDate,

        @Schema(description = "공결 대상 교시", example = "2")
        Byte period,

        @Schema(description = "공결 신청 사유", example = "질병으로 병원 진료를 받았습니다.")
        String reason,

        @Schema(description = "처리 상태", example = "PENDING")
        ExcuseRequestStatus status,

        @Schema(description = "신청 시각", example = "2026-09-02T10:30:00")
        LocalDateTime createdAt
) {

    public static ExcuseRequestResponseDTO from(ExcuseRequest request) {
        return new ExcuseRequestResponseDTO(
                request.getId(),
                request.getEnrollment().getId(),
                request.getLectureDate(),
                request.getPeriod(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
