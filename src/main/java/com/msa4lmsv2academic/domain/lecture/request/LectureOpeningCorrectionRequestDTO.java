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
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "관리자 승인 전 강의 개설 정보 보정")
public record LectureOpeningCorrectionRequestDTO(
        @Schema(description = "보정할 교과목 ID", example = "31")
        @NotNull(message = "보정할 교과목 ID는 필수입니다.")
        @jakarta.validation.constraints.Positive(message = "교과목 ID는 양수여야 합니다.")
        Long courseId,

        @Schema(description = "보정할 학기 ID", example = "5")
        @NotNull(message = "보정할 학기 ID는 필수입니다.")
        @jakarta.validation.constraints.Positive(message = "학기 ID는 양수여야 합니다.")
        Long semesterId,

        @Schema(description = "보정할 분반", example = "01")
        @NotBlank(message = "보정할 분반은 필수입니다.")
        @Pattern(regexp = "[0-9A-Za-z-]{1,10}", message = "분반은 영문, 숫자, 하이픈 10자 이내여야 합니다.")
        String sectionNo,

        @Schema(description = "보정할 수강 정원", example = "40")
        @NotNull(message = "보정할 정원은 필수입니다.")
        @Min(value = 1, message = "정원은 1 이상이어야 합니다.")
        @Max(value = 1000, message = "정원은 1000 이하여야 합니다.")
        Integer requestedCapacity,

        @Schema(description = "보정할 강의실", example = "공학관 301호")
        @NotBlank(message = "보정할 강의실은 필수입니다.")
        @Size(max = 50, message = "강의실은 50자 이하여야 합니다.")
        String classroom,

        @Schema(description = "보정할 중간고사 반영 비율", example = "30")
        @NotNull @Min(0) @Max(100) Integer midtermRatio,
        @Schema(description = "보정할 기말고사 반영 비율", example = "30")
        @NotNull @Min(0) @Max(100) Integer finalRatio,
        @Schema(description = "보정할 과제 반영 비율", example = "30")
        @NotNull @Min(0) @Max(100) Integer assignmentRatio,
        @Schema(description = "보정할 출석 반영 비율", example = "10")
        @NotNull @Min(0) @Max(100) Integer attendanceRatio,

        @Schema(description = "보정할 강의계획서 본문", example = "운영체제의 핵심 개념과 실습을 학습합니다.")
        @NotBlank(message = "보정할 강의계획서는 필수입니다.")
        @Size(max = 65535, message = "강의계획서는 65535자 이하여야 합니다.")
        String syllabus,

        @Schema(description = "보정할 강의 시간표")
        @NotEmpty(message = "보정할 강의 시간표를 하나 이상 입력해야 합니다.")
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
    @AssertTrue(message = "보정 시간표 안에서 강의 시간이 중복될 수 없습니다.")
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
