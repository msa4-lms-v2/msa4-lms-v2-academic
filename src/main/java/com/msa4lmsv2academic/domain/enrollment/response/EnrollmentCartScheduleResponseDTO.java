package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartScheduleQueryResult;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 강의 시간표")
public record EnrollmentCartScheduleResponseDTO(
        @Schema(description = "요일", example = "MON") LectureDayOfWeek dayOfWeek,
        @Schema(description = "시작 교시", example = "1") Byte startPeriod,
        @Schema(description = "종료 교시", example = "2") Byte endPeriod
) {

    public static EnrollmentCartScheduleResponseDTO from(EnrollmentCartScheduleQueryResult result) {
        return new EnrollmentCartScheduleResponseDTO(
                result.dayOfWeek(),
                result.startPeriod(),
                result.endPeriod()
        );
    }
}
