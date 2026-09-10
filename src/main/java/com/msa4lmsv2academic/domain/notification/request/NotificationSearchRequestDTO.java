package com.msa4lmsv2academic.domain.notification.request;

import com.msa4lmsv2academic.domain.notification.entity.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "공통 알림 검색 조건")
public record NotificationSearchRequestDTO(
        @Min(1) @Schema(description = "페이지 번호", example = "1", defaultValue = "1") Integer page,
        @Min(1) @Max(100) @Schema(description = "페이지 크기", example = "20", defaultValue = "20") Integer size,
        @Schema(description = "알림 범주", nullable = true, example = "ACADEMIC") NotificationCategory category,
        @Schema(description = "읽지 않은 알림만 조회", example = "true", defaultValue = "false") Boolean unreadOnly
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
    public boolean resolvedUnreadOnly() { return Boolean.TRUE.equals(unreadOnly); }
}
