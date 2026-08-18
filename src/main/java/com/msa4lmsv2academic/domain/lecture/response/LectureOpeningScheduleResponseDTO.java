package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 개설 신청 시간표")
public record LectureOpeningScheduleResponseDTO(
        @Schema(description = "요일", example = "MON")
        LectureDayOfWeek dayOfWeek,
        @Schema(description = "시작 교시", example = "1")
        Byte startPeriod,
        @Schema(description = "종료 교시", example = "2")
        Byte endPeriod
) {

    public static LectureOpeningScheduleResponseDTO from(LectureOpeningRequestSchedule schedule) {
        return new LectureOpeningScheduleResponseDTO(
                schedule.getDayOfWeek(),
                schedule.getStartPeriod(),
                schedule.getEndPeriod()
        );
    }
}
