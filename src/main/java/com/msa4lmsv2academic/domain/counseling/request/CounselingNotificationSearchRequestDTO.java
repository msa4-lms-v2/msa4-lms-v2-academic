package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "상담 알림 검색 조건")
public record CounselingNotificationSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        @Schema(description = "읽지 않은 알림만 조회할지 여부", example = "true", defaultValue = "false")
        Boolean unreadOnly
) {
    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }

    public boolean resolvedUnreadOnly() {
        return Boolean.TRUE.equals(unreadOnly);
    }
}
