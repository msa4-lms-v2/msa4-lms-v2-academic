package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LeavePeriodSearchRequestDTO(
        @Min(1) @Schema(description = "1부터 시작하는 페이지", defaultValue = "1", example = "1") Integer page,
        @Min(1) @Max(100) @Schema(description = "페이지 크기(1~100)", defaultValue = "20", example = "20") Integer size,
        @Positive @Schema(description = "적용 학기 ID 필터", example = "23") Long semesterId,
        @Schema(description = "신청 유형 필터", example = "GENERAL_LEAVE") LeaveRequestType requestType,
        @Schema(description = "활성 여부 필터. 학생은 활성 설정만 조회", example = "true") Boolean active
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
}
