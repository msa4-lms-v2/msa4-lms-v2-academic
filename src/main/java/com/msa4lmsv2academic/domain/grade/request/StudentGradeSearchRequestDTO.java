package com.msa4lmsv2academic.domain.grade.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "학생 본인 성적 조회 조건")
public record StudentGradeSearchRequestDTO(
        @Schema(description = "학년도. 생략하면 전체 학년도를 조회합니다.", example = "2026")
        @Min(value = 1900, message = "academicYear는 1900 이상이어야 합니다.")
        @Max(value = 9999, message = "academicYear는 9999 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기. 생략하면 전체 학기를 조회합니다.", example = "FIRST",
                allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "교과목명 검색어. 생략하면 전체 교과목을 조회합니다.", example = "운영체제")
        @Size(max = 100, message = "courseName은 100자 이하여야 합니다.")
        String courseName,

        @Schema(description = "정렬 기준", defaultValue = "ACADEMIC_YEAR",
                allowableValues = {"ACADEMIC_YEAR", "COURSE_NAME", "GRADE"})
        StudentGradeSortBy sortBy,

        @Schema(description = "정렬 방향", defaultValue = "DESC", allowableValues = {"ASC", "DESC"})
        StudentGradeSortDirection direction
) {
    public StudentGradeSortBy resolvedSortBy() {
        return sortBy == null ? StudentGradeSortBy.ACADEMIC_YEAR : sortBy;
    }

    public StudentGradeSortDirection resolvedDirection() {
        return direction == null ? StudentGradeSortDirection.DESC : direction;
    }

    public String normalizedCourseName() {
        return courseName == null || courseName.isBlank() ? null : courseName.trim();
    }
}
