package com.msa4lmsv2academic.domain.infochange.request;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

@Schema(description = "프로필 변경 신청 목록 검색 조건")
public record InfoChangeRequestSearchRequestDTO(
        @Schema(description = "신청자 이름 부분 검색", example = "김", nullable = true)
        @Size(max = 50)
        String keyword,

        @Schema(description = "신청 상태", example = "REQUESTED", nullable = true)
        InfoChangeRequestStatus status,

        @Schema(description = "신청자 소속 학과 ID", example = "3", nullable = true)
        @Positive
        Long departmentId,

        @Schema(description = "생성 시각 정렬 방향", example = "DESC", defaultValue = "DESC")
        Sort.Direction sortDirection,

        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(1)
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(1)
        Integer size
) {
    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public Sort.Direction resolvedSortDirection() {
        return sortDirection == null ? Sort.Direction.DESC : sortDirection;
    }

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }
}
