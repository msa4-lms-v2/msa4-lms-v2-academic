package com.msa4lmsv2academic.domain.lecture.request;

import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "강의 개설 신청 목록 조회 조건")
public record LectureOpeningSearchRequestDTO(
        @Schema(description = "처리 상태", example = "PENDING")
        LectureOpeningRequestStatus status,

        @Schema(description = "페이지 번호(1부터 시작)", example = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기", example = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size
) {

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : Math.min(size, 100);
    }
}
