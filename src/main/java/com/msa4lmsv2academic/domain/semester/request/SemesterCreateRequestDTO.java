package com.msa4lmsv2academic.domain.semester.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "학기 등록 요청")
public record SemesterCreateRequestDTO(
        @Schema(description = "학년도", example = "2026", minimum = "1", maximum = "32767")
        @NotNull(message = "academicYear는 필수입니다.")
        @Min(value = 1, message = "academicYear는 1 이상이어야 합니다.")
        @Max(value = Short.MAX_VALUE, message = "academicYear는 32767 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기 구분", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        @NotNull(message = "term은 필수입니다.")
        SemesterTerm term,

        @Schema(description = "개강일", example = "2026-03-02", format = "date")
        @NotNull(message = "startDate는 필수입니다.")
        LocalDate startDate,

        @Schema(description = "종강일", example = "2026-06-19", format = "date")
        @NotNull(message = "endDate는 필수입니다.")
        LocalDate endDate,

        @Schema(description = "수강신청 시작 일시", example = "2026-02-16T09:00:00", format = "date-time")
        @NotNull(message = "enrollmentStartAt은 필수입니다.")
        LocalDateTime enrollmentStartAt,

        @Schema(description = "수강신청 종료 일시", example = "2026-02-20T18:00:00", format = "date-time")
        @NotNull(message = "enrollmentEndAt은 필수입니다.")
        LocalDateTime enrollmentEndAt,

        @Schema(description = "현재 학기 여부. 생략하면 false이며, true이면 기존 현재 학기를 자동 해제합니다.",
                example = "true", defaultValue = "false")
        Boolean isCurrent
) {

    @Schema(hidden = true)
    @AssertTrue(message = "startDate는 endDate보다 빨라야 하고 enrollmentStartAt은 enrollmentEndAt보다 빨라야 합니다.")
    public boolean isPeriodOrderValid() {
        boolean classPeriodValid = startDate == null || endDate == null || startDate.isBefore(endDate);
        boolean enrollmentPeriodValid = enrollmentStartAt == null || enrollmentEndAt == null
                || enrollmentStartAt.isBefore(enrollmentEndAt);
        return classPeriodValid && enrollmentPeriodValid;
    }

    public boolean resolvedCurrent() {
        return Boolean.TRUE.equals(isCurrent);
    }
}
