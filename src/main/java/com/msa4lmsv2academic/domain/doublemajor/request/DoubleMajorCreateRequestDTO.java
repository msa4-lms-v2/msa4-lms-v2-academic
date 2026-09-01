package com.msa4lmsv2academic.domain.doublemajor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DoubleMajorCreateRequestDTO(
        @NotNull @Positive
        @Schema(description = "희망 복수전공 ID. 희망 학과는 이 전공의 소속 학과로 결정됩니다.", example = "126",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetMajorId
) { }
