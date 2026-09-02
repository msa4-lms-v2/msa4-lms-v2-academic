package com.msa4lmsv2academic.domain.doublemajor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DoubleMajorCreateRequestDTO(
        @NotNull @Positive
        @Schema(description = "희망 복수전공 학과 ID", example = "249",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Long targetDepartmentId
) { }
