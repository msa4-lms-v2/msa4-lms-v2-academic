package com.msa4lmsv2academic.domain.counseling.request;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "상담 예약 상태 및 교수 메모 변경 요청")
public record CounselingAppointmentStatusRequestDTO(
        @Schema(description = "변경할 상담 상태", example = "CONFIRMED")
        @NotNull
        CounselingAppointmentStatus status,

        @Schema(description = "교수의 승인·반려 사유 또는 온라인 상담 답변", example = "수강 계획을 먼저 확인해 주세요.")
        @Size(max = 5000, message = "교수 메모는 5000자 이하여야 합니다.")
        String professorNote
) {
}
