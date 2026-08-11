package com.msa4lmsv2academic.domain.counseling.request;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "학생 본인 상담 기록 검색 조건")
public record StudentCounselingRecordSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "상담 방식", example = "ONLINE")
        CounselingMethod counselingMethod,

        @Schema(description = "상담 상태", example = "COMPLETED")
        CounselingStatus status
) {

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : Math.min(size, 100);
    }
}
