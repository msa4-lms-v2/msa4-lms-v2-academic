package com.msa4lmsv2academic.domain.grade.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GradeCorrectionHistorySearchRequestDTO(
        @Schema(description = "강의 ID", example = "101")
        @NotNull @Positive Long classId,
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(1) Integer page,
        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(1) Integer size
) {
    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }
}
