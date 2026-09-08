package com.msa4lmsv2academic.domain.counseling.request;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CounselingSearchRequestDTO(
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer size,
        CounselingStatus status
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
}
