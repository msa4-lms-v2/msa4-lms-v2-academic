package com.msa4lmsv2academic.domain.attendance.request;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "공결 처리 상태 조회 조건")
public record ExcuseRequestSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기. 100을 초과하면 100으로 제한", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(
                description = "처리 상태. 생략하면 모든 상태를 조회합니다.",
                example = "PENDING",
                allowableValues = {"PENDING", "APPROVED", "REJECTED"}
        )
        ExcuseRequestStatus status
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
