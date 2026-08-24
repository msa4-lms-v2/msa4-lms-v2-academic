package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "선수과목·재수강 기준정보 및 개인별 판정 조회 조건")
public record PrerequisiteRetakeRuleSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "대상·선수 교과목 코드 또는 이름", example = "자료구조", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        @Schema(description = "판정할 대상 교과목 ID. 학생·교수 판정 시 필수", example = "20")
        @Positive(message = "courseId는 양수여야 합니다.")
        Long courseId,

        @Schema(description = "판정 대상 학생 ID. PROFESSOR·ADMIN 개인별 판정 시 사용", example = "8")
        @Positive(message = "studentId는 양수여야 합니다.")
        Long studentId,

        @Schema(description = "기준 활성 여부 필터. ADMIN 기준정보 조회에서만 사용", example = "true")
        Boolean active,

        @Schema(description = "정렬 필드", defaultValue = "courseCode",
                allowableValues = {"courseCode", "prerequisiteCourseCode", "createdAt", "updatedAt"})
        @Pattern(
                regexp = "courseCode|prerequisiteCourseCode|createdAt|updatedAt",
                message = "sortBy는 courseCode, prerequisiteCourseCode, createdAt, updatedAt 중 하나여야 합니다."
        )
        String sortBy,

        @Schema(description = "정렬 방향", defaultValue = "asc", allowableValues = {"asc", "desc"})
        @Pattern(regexp = "asc|desc", message = "sortDirection은 asc 또는 desc여야 합니다.")
        String sortDirection
) {

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }

    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    public String resolvedSortBy() {
        return sortBy == null ? "courseCode" : sortBy;
    }

    public boolean descending() {
        return "desc".equals(sortDirection);
    }
}
