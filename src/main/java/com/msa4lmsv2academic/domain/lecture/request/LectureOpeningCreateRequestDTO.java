package com.msa4lmsv2academic.domain.lecture.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "교수 강의 개설 신청")
public record LectureOpeningCreateRequestDTO(
        @Schema(description = "교과목 ID", example = "31")
        @NotNull(message = "교과목 ID는 필수입니다.")
        @Positive(message = "교과목 ID는 양수여야 합니다.")
        Long courseId,

        @Schema(description = "학기 ID", example = "5")
        @NotNull(message = "학기 ID는 필수입니다.")
        @Positive(message = "학기 ID는 양수여야 합니다.")
        Long semesterId,

        @Schema(description = "분반", example = "01")
        @NotBlank(message = "분반은 필수입니다.")
        @Pattern(regexp = "[0-9A-Za-z-]{1,10}", message = "분반은 영문, 숫자, 하이픈 10자 이내여야 합니다.")
        String sectionNo,

        @Schema(description = "신청 정원", example = "40")
        @NotNull(message = "신청 정원은 필수입니다.")
        @Min(value = 1, message = "신청 정원은 1 이상이어야 합니다.")
        @Max(value = 1000, message = "신청 정원은 1000 이하여야 합니다.")
        Integer requestedCapacity,

        @Schema(description = "강의실", example = "공학관 301호")
        @NotBlank(message = "강의실은 필수입니다.")
        @Size(max = 50, message = "강의실은 50자 이하여야 합니다.")
        String classroom,

        @Schema(description = "중간고사 반영 비율", example = "30")
        @NotNull(message = "중간고사 반영 비율은 필수입니다.")
        @Min(value = 0, message = "중간고사 반영 비율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "중간고사 반영 비율은 100 이하여야 합니다.")
        Integer midtermRatio,

        @Schema(description = "기말고사 반영 비율", example = "30")
        @NotNull(message = "기말고사 반영 비율은 필수입니다.")
        @Min(value = 0, message = "기말고사 반영 비율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "기말고사 반영 비율은 100 이하여야 합니다.")
        Integer finalRatio,

        @Schema(description = "과제 반영 비율", example = "30")
        @NotNull(message = "과제 반영 비율은 필수입니다.")
        @Min(value = 0, message = "과제 반영 비율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "과제 반영 비율은 100 이하여야 합니다.")
        Integer assignmentRatio,

        @Schema(description = "출석 반영 비율", example = "10")
        @NotNull(message = "출석 반영 비율은 필수입니다.")
        @Min(value = 0, message = "출석 반영 비율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "출석 반영 비율은 100 이하여야 합니다.")
        Integer attendanceRatio,

        @Schema(description = "강의계획서 본문", example = "운영체제의 핵심 개념과 실습을 학습합니다.")
        @NotBlank(message = "강의계획서는 필수입니다.")
        @Size(max = 65535, message = "강의계획서는 65535자 이하여야 합니다.")
        String syllabus,

        @Schema(description = "강의 시간표")
        @NotEmpty(message = "강의 시간표를 하나 이상 입력해야 합니다.")
        @Size(max = 10, message = "강의 시간표는 10개 이하여야 합니다.")
        List<@Valid LectureOpeningScheduleRequestDTO> schedules
) {

    @Schema(hidden = true)
    @AssertTrue(message = "성적 반영 비율의 합은 100이어야 합니다.")
    public boolean isRatioTotalValid() {
        if (midtermRatio == null || finalRatio == null || assignmentRatio == null || attendanceRatio == null) {
            return true;
        }
        return midtermRatio + finalRatio + assignmentRatio + attendanceRatio == 100;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "한 신청 안에서 강의 시간이 중복될 수 없습니다.")
    public boolean isScheduleOverlapFree() {
        if (schedules == null) {
            return true;
        }
        Set<String> occupiedPeriods = new HashSet<>();
        for (LectureOpeningScheduleRequestDTO schedule : schedules) {
            if (schedule == null || schedule.dayOfWeek() == null
                    || schedule.startPeriod() == null || schedule.endPeriod() == null
                    || schedule.startPeriod() > schedule.endPeriod()) {
                continue;
            }
            for (int period = schedule.startPeriod(); period <= schedule.endPeriod(); period++) {
                if (!occupiedPeriods.add(schedule.dayOfWeek() + ":" + period)) {
                    return false;
                }
            }
        }
        return true;
    }
}
