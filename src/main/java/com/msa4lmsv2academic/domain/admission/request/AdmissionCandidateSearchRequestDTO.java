package com.msa4lmsv2academic.domain.admission.request;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "입학 예정자 목록 검색·필터·정렬 조건")
public record AdmissionCandidateSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기. 100을 초과하면 100으로 제한", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "이름 또는 수험번호의 대소문자 무시 부분 검색", example = "김민수", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        @Schema(description = "학과 ID 정확 일치", example = "1")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "입학 연도 정확 일치", example = "2027", minimum = "1900")
        @Min(value = 1900, message = "admissionYear는 1900 이상이어야 합니다.")
        Integer admissionYear,

        @Schema(description = "등록 상태 정확 일치", example = "REGISTERED")
        AdmissionCandidateStatus status,

        @Schema(description = "정렬 필드", example = "createdAt", defaultValue = "createdAt",
                allowableValues = {"createdAt", "name", "applicationNumber", "admissionYear"})
        @Pattern(regexp = "createdAt|name|applicationNumber|admissionYear",
                message = "sortBy는 createdAt, name, applicationNumber, admissionYear 중 하나여야 합니다.")
        String sortBy,

        @Schema(description = "정렬 방향", example = "desc", defaultValue = "desc",
                allowableValues = {"asc", "desc"})
        @Pattern(regexp = "asc|desc", message = "sortDirection은 asc 또는 desc여야 합니다.")
        String sortDirection
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

    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    public String resolvedSortBy() {
        return sortBy == null ? "createdAt" : sortBy;
    }

    public boolean descending() {
        return sortDirection == null || "desc".equals(sortDirection);
    }
}
