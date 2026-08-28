package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LeaveRequestCreateRequestDTO(
        @NotNull @Schema(description = "신청 유형. 복학 유형은 현재 휴학 근거로 서버가 판별합니다.", example = "GENERAL_LEAVE", requiredMode = Schema.RequiredMode.REQUIRED)
        LeaveRequestType requestType,
        @Size(max = 500) @Schema(description = "일반휴학 필수 사유(1~500자). 군휴학·복학은 생략 시 기본 사유 사용", maxLength = 500, example = "개인 사정")
        String reason,
        @NotNull @Min(1) @Max(32767) @Schema(description = "적용 학년도", example = "2027", requiredMode = Schema.RequiredMode.REQUIRED)
        Short targetYear,
        @NotNull @Min(1) @Max(2) @Schema(description = "적용 학기(1 또는 2)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Byte targetSemester,
        @Min(1) @Max(32767) @Schema(description = "일반휴학 필수 복학 예정 학년도. 군휴학·복학에서는 보내지 않습니다.", nullable = true, example = "2028")
        Short returnYear,
        @Min(1) @Max(2) @Schema(description = "일반휴학 필수 복학 예정 학기. 휴학 시작 학기보다 뒤여야 합니다.", nullable = true, example = "1")
        Byte returnSemester
) {
    public LeaveRequestCreateRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
