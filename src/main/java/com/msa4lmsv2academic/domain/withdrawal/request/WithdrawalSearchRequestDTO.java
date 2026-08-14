package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "자퇴 신청 목록 검색 조건")
public record WithdrawalSearchRequestDTO(
        @Min(1) Integer page,
        @Min(1) Integer size
) {
    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }
}
