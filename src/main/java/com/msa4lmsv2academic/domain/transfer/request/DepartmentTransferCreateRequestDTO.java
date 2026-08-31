package com.msa4lmsv2academic.domain.transfer.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DepartmentTransferCreateRequestDTO(
        @NotNull @Positive
        @Schema(description = "희망 학과 ID. 현재 학과와 달라야 하며 활성 상태여야 합니다.", example = "20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetDepartmentId,
        @NotNull @Positive
        @Schema(description = "희망 주전공 ID. 희망 학과 소속의 활성 전공이어야 합니다.", example = "31",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetMajorId,
        @NotNull @Positive
        @Schema(description = "적용 희망 학기 ID. 활성 접수 기간이 등록되어 있어야 합니다.", example = "23",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetSemesterId,
        @NotBlank @Size(max = 500)
        @Schema(description = "전과 신청 사유(1~500자)", minLength = 1, maxLength = 500,
                example = "경영 분야로 진로를 변경하고자 신청합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
    public DepartmentTransferCreateRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
