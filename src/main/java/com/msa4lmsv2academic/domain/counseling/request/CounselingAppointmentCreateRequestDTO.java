package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "상담 예약 요청")
public record CounselingAppointmentCreateRequestDTO(
        @NotNull
        @Positive
        @Schema(description = "예약할 교수 ID", example = "31")
        Long professorId,

        @NotNull
        @Schema(description = "예약 시작 시각", example = "2026-09-07T09:30:00")
        LocalDateTime appointmentAt,

        @Size(max = 255, message = "상담 주제는 255자 이하여야 합니다.")
        @Schema(description = "상담 주제")
        String topic
) {
}
