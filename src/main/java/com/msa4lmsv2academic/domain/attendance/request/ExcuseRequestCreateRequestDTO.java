package com.msa4lmsv2academic.domain.attendance.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "학생 공결 신청 요청")
public record ExcuseRequestCreateRequestDTO(
        @NotNull(message = "수강신청 ID는 필수입니다.")
        @Positive(message = "수강신청 ID는 양수여야 합니다.")
        @Schema(description = "공결을 신청할 본인 수강신청 ID", example = "12001")
        Long enrollmentId,

        @NotNull(message = "결석 수업일은 필수입니다.")
        @Schema(description = "공결 대상 수업일", example = "2026-09-01")
        LocalDate lectureDate,

        @NotNull(message = "교시는 필수입니다.")
        @Min(value = 1, message = "교시는 1 이상이어야 합니다.")
        @Max(value = 20, message = "교시는 20 이하여야 합니다.")
        @Schema(description = "공결 대상 교시", example = "2", minimum = "1", maximum = "20")
        Byte period,

        @NotBlank(message = "공결 사유는 필수입니다.")
        @Size(max = 500, message = "공결 사유는 500자 이하여야 합니다.")
        @Schema(description = "공결 신청 사유", example = "질병으로 병원 진료를 받았습니다.", maxLength = 500)
        String reason
) {
}
