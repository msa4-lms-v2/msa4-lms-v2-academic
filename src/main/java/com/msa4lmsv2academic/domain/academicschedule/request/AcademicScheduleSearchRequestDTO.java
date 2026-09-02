package com.msa4lmsv2academic.domain.academicschedule.request;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "학사일정 목록 검색 조건")
public record AcademicScheduleSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "제목·본문 검색어", example = "수강신청", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        @Schema(description = "조회 기간 시작일. 이 날짜 이후까지 이어지는 일정을 조회합니다.",
                example = "2026-08-01", format = "date")
        LocalDate from,

        @Schema(description = "조회 기간 종료일. 이 날짜 이전에 시작한 일정을 조회합니다.",
                example = "2026-08-31", format = "date")
        LocalDate to,

        @Schema(description = "공개 대상 역할. 일반 사용자는 ALL 또는 본인 역할만 지정할 수 있습니다.",
                example = "STUDENT", allowableValues = {"ALL", "STUDENT", "PROFESSOR"})
        AcademicScheduleTargetRole targetRole,

        @Schema(description = "활성 상태. ADMIN만 선택할 수 있으며 생략하면 전체 상태를 조회합니다.", example = "true")
        Boolean active
) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public int resolvedPage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? DEFAULT_SIZE : size, MAX_SIZE);
    }

    @AssertTrue(message = "from은 to보다 늦을 수 없습니다.")
    @Schema(hidden = true)
    public boolean isValidDateRange() {
        return from == null || to == null || !from.isAfter(to);
    }
}
