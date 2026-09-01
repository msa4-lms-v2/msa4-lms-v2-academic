package com.msa4lmsv2academic.domain.doublemajor.request;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestStatus;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferRequestSort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record DoubleMajorSearchRequestDTO(
        @Min(1) @Schema(description = "1부터 시작하는 페이지", defaultValue = "1", example = "1") Integer page,
        @Min(1) @Max(100) @Schema(description = "페이지 크기(1~100)", defaultValue = "20", example = "20") Integer size,
        @Schema(description = "처리 상태", example = "PENDING") AcademicChangeRequestStatus status,
        @Positive @Schema(description = "모집 회차 ID", example = "2") Long requestPeriodId,
        @Positive @Schema(description = "희망 학과 ID", example = "249") Long targetDepartmentId,
        @Positive @Schema(description = "희망 복수전공 ID", example = "126") Long targetMajorId,
        @Positive @Schema(description = "학생 ID. 학생은 본인 ID만, 관리자는 전체 검색 가능", example = "1") Long studentId,
        @Size(max = 100)
        @Schema(description = "학생명·신청 당시/희망 학과·전공명 검색(최대 100자)", example = "경영") String keyword,
        @Schema(description = "생성 시각 정렬", defaultValue = "CREATED_AT_DESC", example = "CREATED_AT_DESC")
        DepartmentTransferRequestSort sort
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
    public boolean ascending() { return sort == DepartmentTransferRequestSort.CREATED_AT_ASC; }
    public String normalizedKeyword() { return keyword == null || keyword.isBlank() ? null : keyword.strip(); }
}
