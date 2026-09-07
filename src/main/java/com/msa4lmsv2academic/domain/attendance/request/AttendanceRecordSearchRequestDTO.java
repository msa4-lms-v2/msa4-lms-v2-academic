package com.msa4lmsv2academic.domain.attendance.request;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

@Schema(description = "출결 기록 조회 조건")
public record AttendanceRecordSearchRequestDTO(
        @Schema(description = "개설 강의 ID", example = "101")
        @Positive(message = "classId는 양수여야 합니다.")
        Long classId,

        @Schema(description = "수강 ID", example = "12001")
        @Positive(message = "enrollmentId는 양수여야 합니다.")
        Long enrollmentId,

        @Schema(description = "조회 시작 수업일", example = "2026-09-01")
        LocalDate fromDate,

        @Schema(description = "조회 종료 수업일", example = "2026-09-30")
        LocalDate toDate,

        @Schema(
                description = "출결 상태. 생략하면 모든 상태를 조회합니다.",
                example = "PRESENT",
                allowableValues = {"PRESENT", "LATE", "ABSENT", "EXCUSED"}
        )
        AttendanceStatus status,

        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기. 100을 초과하면 100으로 제한", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size
) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @AssertTrue(message = "조회 시작일은 종료일보다 늦을 수 없습니다.")
    @Schema(hidden = true)
    public boolean isValidDateRange() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }

    public int resolvedPage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? DEFAULT_SIZE : size, MAX_SIZE);
    }
}
