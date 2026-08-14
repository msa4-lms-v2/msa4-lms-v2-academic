package com.msa4lmsv2academic.domain.counseling.request;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "상담 예약 검색 조건")
public record CounselingAppointmentSearchRequestDTO(
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        Integer size,

        CounselingAppointmentStatus status
) {
    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
