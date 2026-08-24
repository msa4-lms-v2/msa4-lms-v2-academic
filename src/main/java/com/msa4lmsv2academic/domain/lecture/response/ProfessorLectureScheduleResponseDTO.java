package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureScheduleQueryResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교수 담당 강의 시간표")
public record ProfessorLectureScheduleResponseDTO(
        @Schema(description = "요일", example = "MON")
        LectureDayOfWeek dayOfWeek,
        @Schema(description = "시작 교시", example = "1")
        Byte startPeriod,
        @Schema(description = "종료 교시", example = "2")
        Byte endPeriod
) {

    public static ProfessorLectureScheduleResponseDTO from(ProfessorLectureScheduleQueryResult result) {
        return new ProfessorLectureScheduleResponseDTO(
                result.dayOfWeek(),
                result.startPeriod(),
                result.endPeriod()
        );
    }
}
