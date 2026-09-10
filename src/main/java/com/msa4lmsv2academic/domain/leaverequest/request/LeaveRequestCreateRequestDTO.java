package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LeaveRequestCreateRequestDTO(
        @NotNull @Schema(description = "신청 유형. 복학은 일반복학 또는 군복학을 선택하며, 서버가 현재 승인 휴학 이력과 일치 여부를 검증합니다.", example = "GENERAL_LEAVE", requiredMode = Schema.RequiredMode.REQUIRED)
        LeaveRequestType requestType,
        @Size(max = 500) @Schema(description = "일반휴학 필수 사유(1~500자). 군휴학·복학은 생략 시 기본 사유 사용", maxLength = 500, example = "개인 사정")
        String reason,
        @Min(1) @Max(32767) @Schema(description = "적용 학년도. 일반휴학·복학에는 필수이며, 군휴학은 서버가 현재 학기로 결정해 클라이언트 값은 사용하지 않습니다.", nullable = true, example = "2027")
        Short targetYear,
        @Min(1) @Max(2) @Schema(description = "적용 학기(1 또는 2). 일반휴학·복학에는 필수이며, 군휴학은 서버가 현재 학기로 결정해 클라이언트 값은 사용하지 않습니다.", nullable = true, example = "1")
        Byte targetSemester,
        @Min(1) @Max(32767) @Schema(description = "일반휴학 필수 복학 예정 학년도. 적용 학기의 바로 다음 학기여야 하며, 군휴학·복학에서는 보내지 않습니다.", nullable = true, example = "2028")
        Short returnYear,
        @Min(1) @Max(2) @Schema(description = "일반휴학 필수 복학 예정 학기. 적용 학기의 바로 다음 학기여야 합니다.", nullable = true, example = "1")
        Byte returnSemester
) {
    public LeaveRequestCreateRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
