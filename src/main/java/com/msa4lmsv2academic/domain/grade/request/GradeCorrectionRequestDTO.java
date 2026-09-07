package com.msa4lmsv2academic.domain.grade.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GradeCorrectionRequestDTO(
        @NotNull @Positive Long classId,
        @NotEmpty @Size(max = 500) List<@Valid GradeCorrectionItemRequestDTO> corrections
) {
}
