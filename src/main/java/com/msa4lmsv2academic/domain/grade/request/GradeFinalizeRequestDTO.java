package com.msa4lmsv2academic.domain.grade.request;

import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import jakarta.validation.constraints.NotNull;

public record GradeFinalizeRequestDTO(@NotNull GradeStatus status) {
}
