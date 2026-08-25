package com.msa4lmsv2academic.domain.enrollment.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "최대 신청학점 규칙 목록 검색 조건")
public record EnrollmentCreditLimitRuleSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "학년도", example = "2027", minimum = "1", maximum = "32767")
        @Min(value = 1, message = "academicYear는 1 이상이어야 합니다.")
        @Max(value = Short.MAX_VALUE, message = "academicYear는 32767 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기 구분", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "활성 상태", example = "true")
        Boolean active,

        @Schema(description = "정렬 필드", defaultValue = "academicYear",
                allowableValues = {"academicYear", "maxCredits", "createdAt", "updatedAt"})
        @Pattern(regexp = "academicYear|maxCredits|createdAt|updatedAt",
                message = "sortBy는 academicYear, maxCredits, createdAt, updatedAt 중 하나여야 합니다.")
        String sortBy,

        @Schema(description = "정렬 방향", defaultValue = "desc", allowableValues = {"asc", "desc"})
        @Pattern(regexp = "asc|desc", message = "sortDirection은 asc 또는 desc여야 합니다.")
        String sortDirection
) {

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }

    public String resolvedSortBy() {
        return sortBy == null ? "academicYear" : sortBy;
    }

    public boolean descending() {
        return sortDirection == null || "desc".equals(sortDirection);
    }
}
