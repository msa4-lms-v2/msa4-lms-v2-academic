package com.msa4lmsv2academic.domain.graduation.request;

import com.msa4lmsv2academic.domain.graduation.entity.CreditDiagnosisStatus;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "역할 범위별 학점 진단 현황 검색 조건")
public record CreditDiagnosisSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "학생 이름 검색", example = "김학생", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        @Schema(description = "학과 ID", example = "1")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "입학연도", example = "2024", minimum = "1900")
        @Min(value = 1900, message = "admissionYear는 1900 이상이어야 합니다.")
        Integer admissionYear,

        @Schema(description = "학적 상태", example = "ENROLLED")
        AcademicStatus academicStatus,

        @Schema(description = "진단 상태", example = "NOT_SATISFIED")
        CreditDiagnosisStatus diagnosisStatus,

        @Schema(description = "정렬 필드", defaultValue = "studentName",
                allowableValues = {"studentName", "departmentName", "admissionYear"})
        @Pattern(regexp = "studentName|departmentName|admissionYear",
                message = "sortBy는 studentName, departmentName, admissionYear 중 하나여야 합니다.")
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
        return sortBy == null ? "studentName" : sortBy;
    }

    public boolean descending() {
        return "desc".equals(sortDirection);
    }
}
