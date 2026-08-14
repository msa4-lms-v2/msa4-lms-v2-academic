package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Schema(description = "교수 상담 가능 시간 슬롯")
public record CounselorAvailabilitySlotRequestDTO(
        @NotNull
        @Schema(description = "요일", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @NotBlank
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "startTime은 HH:mm 형식이어야 합니다.")
        @Schema(description = "시작 시각", example = "09:00")
        String startTime,

        @NotBlank
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "endTime은 HH:mm 형식이어야 합니다.")
        @Schema(description = "종료 시각", example = "12:00")
        String endTime,

        @NotNull
        @Schema(description = "유효 시작일", example = "2026-09-01")
        LocalDate validFrom,

        @Schema(description = "유효 종료일", example = "2026-12-18")
        LocalDate validTo
) {
}
