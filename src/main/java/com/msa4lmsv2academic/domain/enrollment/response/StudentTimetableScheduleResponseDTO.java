package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableScheduleQueryResult;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학생 시간표의 강의 시간")
public record StudentTimetableScheduleResponseDTO(
        @Schema(description = "요일", example = "MON") LectureDayOfWeek dayOfWeek,
        @Schema(description = "시작 교시", example = "1") Byte startPeriod,
        @Schema(description = "종료 교시", example = "2") Byte endPeriod
) {

    public static StudentTimetableScheduleResponseDTO from(StudentTimetableScheduleQueryResult result) {
        return new StudentTimetableScheduleResponseDTO(
                result.dayOfWeek(),
                result.startPeriod(),
                result.endPeriod()
        );
    }
}
