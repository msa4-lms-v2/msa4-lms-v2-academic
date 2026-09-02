package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "교수 상담 가능 시간 전체 교체 요청")
public record CounselorAvailabilityReplaceRequestDTO(
        @NotNull
        @Size(max = 50, message = "상담 가능 시간은 최대 50개까지 등록할 수 있습니다.")
        List<@Valid CounselorAvailabilitySlotRequestDTO> slots
) {
}
