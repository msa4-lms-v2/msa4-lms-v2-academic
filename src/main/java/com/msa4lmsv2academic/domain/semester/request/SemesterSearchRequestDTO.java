package com.msa4lmsv2academic.domain.semester.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "학기 목록 검색 조건")
public record SemesterSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "학년도", example = "2026", minimum = "1", maximum = "32767")
        @Min(value = 1, message = "academicYear는 1 이상이어야 합니다.")
        @Max(value = Short.MAX_VALUE, message = "academicYear는 32767 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기 구분", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "현재 학기 여부", example = "true")
        Boolean isCurrent
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
}
