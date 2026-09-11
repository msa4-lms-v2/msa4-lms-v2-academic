package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequesterType;
import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자용 학생·교수 통합 프로필 변경 신청 목록 항목")
public record AdminInfoChangeRequestSummaryResponseDTO(
        @Schema(description = "신청자 유형", example = "STUDENT") InfoChangeRequesterType requesterType,
        @Schema(description = "유형별 신청 ID", example = "12") Long requestId,
        @Schema(description = "신청자 이름", example = "김학생") String requesterName,
        @Schema(description = "소속 학과 ID", example = "3", nullable = true) Long departmentId,
        @Schema(description = "소속 학과명", example = "컴퓨터공학과", nullable = true) String departmentName,
        @Schema(description = "처리 상태", example = "REQUESTED") InfoChangeRequestStatus status,
        @Schema(description = "신청 일시", example = "2026-09-11T10:30:00") LocalDateTime createdAt
) {
}
