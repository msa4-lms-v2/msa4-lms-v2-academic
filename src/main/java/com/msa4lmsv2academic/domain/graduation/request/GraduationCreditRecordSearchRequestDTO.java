package com.msa4lmsv2academic.domain.graduation.request;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditRecordResult;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "졸업학점 과목별 수강·성적 기록 검색 조건")
public record GraduationCreditRecordSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "학년도", example = "2024", minimum = "1900")
        @Min(value = 1900, message = "academicYear는 1900 이상이어야 합니다.")
        Integer academicYear,

        @Schema(description = "학기", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED",
                allowableValues = {"MAJOR_REQUIRED", "MAJOR_ELECTIVE", "GENERAL_REQUIRED", "GENERAL_ELECTIVE"})
        CompletionType completionType,

        @Schema(description = "졸업학점 반영 결과", example = "EXCLUDED",
                allowableValues = {"APPLIED", "EXCLUDED"})
        GraduationCreditRecordResult result,

        @Schema(description = "최신순 또는 과거순", example = "desc", defaultValue = "desc",
                allowableValues = {"asc", "desc"})
        @Pattern(regexp = "asc|desc", message = "sortDirection은 asc 또는 desc여야 합니다.")
        String sortDirection
) {

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }

    public boolean ascending() {
        return "asc".equals(sortDirection);
    }
}
