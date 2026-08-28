package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "수강 장바구니 추가 요청. 학생 식별자는 인증 정보에서 결정합니다.")
public record EnrollmentCartCreateRequestDTO(
        @Schema(description = "장바구니에 담을 개설 강의 ID (교과목 ID 아님)", example = "20",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @NotNull
        @Positive
        Long lectureId
) {
}
