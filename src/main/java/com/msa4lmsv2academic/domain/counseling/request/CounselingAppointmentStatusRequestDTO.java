package com.msa4lmsv2academic.domain.counseling.request;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "상담 예약 상태 및 교수 메모 변경 요청")
public record CounselingAppointmentStatusRequestDTO(
        @NotNull
        CounselingAppointmentStatus status,

        @Size(max = 5000, message = "교수 메모는 5000자 이하여야 합니다.")
        String professorNote
) {
}
