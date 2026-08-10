package com.msa4lmsv2academic.domain.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "학과 목록 검색 조건")
public record DepartmentSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "단과대 ID")
        @Positive(message = "collegeId는 양수여야 합니다.")
        Long collegeId,

        @Schema(description = "활성 여부. ADMIN만 비활성 학과를 조회할 수 있습니다.")
        Boolean active,

        @Schema(description = "학과 코드 정확 일치 또는 학과명 부분 검색", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword
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
