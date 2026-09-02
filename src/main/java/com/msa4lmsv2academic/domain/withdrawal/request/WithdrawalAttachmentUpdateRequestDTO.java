package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "자퇴 증빙 등록·교체 사유. 학생 최초 등록은 생략할 수 있습니다.")
public record WithdrawalAttachmentUpdateRequestDTO(
        @Schema(description = "증빙 변경 사유. 학생 최초 등록은 서버가 자동 기록하며, 학생 교체와 관리자의 모든 변경에는 필수",
                nullable = true, minLength = 1, maxLength = 255,
                example = "개인정보를 가린 파일로 정정합니다.")
        @Size(max = 255) String changeReason
) {
    public WithdrawalAttachmentUpdateRequestDTO {
        changeReason = changeReason == null ? null : changeReason.strip();
    }
}
