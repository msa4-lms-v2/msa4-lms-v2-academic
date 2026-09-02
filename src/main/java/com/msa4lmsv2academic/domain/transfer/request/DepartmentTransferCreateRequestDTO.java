package com.msa4lmsv2academic.domain.transfer.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepartmentTransferCreateRequestDTO(
        @NotNull @Positive
        @Schema(description = "희망 학과 ID. 현재 학과와 달라야 하며 활성 상태여야 합니다.", example = "20",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetDepartmentId,
        @NotNull @Positive
        @Schema(description = "적용 희망 학기 ID. 활성 접수 기간이 등록되어 있어야 합니다.", example = "23",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetSemesterId
) { }
