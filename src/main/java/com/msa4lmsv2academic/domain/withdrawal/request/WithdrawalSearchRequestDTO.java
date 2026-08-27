package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "자퇴 목록. 생성 시각 내림차순, 같은 시각은 ID 내림차순. 모든 상태 포함.")
public record WithdrawalSearchRequestDTO(
        @Schema(description = "1부터 시작하는 페이지", minimum = "1", defaultValue = "1", example = "1")
        @Min(1) Integer page,
        @Schema(description = "페이지 크기. 100 초과는 100으로 제한", minimum = "1", defaultValue = "20", example = "20")
        @Min(1) Integer size
) {
    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }
}
