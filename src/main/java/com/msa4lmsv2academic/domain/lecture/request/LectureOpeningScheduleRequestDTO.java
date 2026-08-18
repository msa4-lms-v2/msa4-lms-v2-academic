package com.msa4lmsv2academic.domain.lecture.request;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "강의 개설 신청 시간표")
public record LectureOpeningScheduleRequestDTO(
        @Schema(description = "요일", example = "MON")
        @NotNull(message = "요일은 필수입니다.")
        LectureDayOfWeek dayOfWeek,

        @Schema(description = "시작 교시", example = "1")
        @NotNull(message = "시작 교시는 필수입니다.")
        @Min(value = 1, message = "시작 교시는 1 이상이어야 합니다.")
        @Max(value = 20, message = "시작 교시는 20 이하여야 합니다.")
        Byte startPeriod,

        @Schema(description = "종료 교시", example = "2")
        @NotNull(message = "종료 교시는 필수입니다.")
        @Min(value = 1, message = "종료 교시는 1 이상이어야 합니다.")
        @Max(value = 20, message = "종료 교시는 20 이하여야 합니다.")
        Byte endPeriod
) {

    @Schema(hidden = true)
    @AssertTrue(message = "시작 교시는 종료 교시보다 클 수 없습니다.")
    public boolean isPeriodOrderValid() {
        return startPeriod == null || endPeriod == null || startPeriod <= endPeriod;
    }
}
