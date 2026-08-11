package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "상담 기록 목록 항목")
public record CounselingRecordSummaryResponseDTO(
        @Schema(description = "상담 기록 ID") Long id,
        @Schema(description = "학생 ID") Long studentId,
        @Schema(description = "학생 이름") String studentName,
        @Schema(description = "상담 방식") CounselingMethod counselingMethod,
        @Schema(description = "상담 상태") CounselingStatus status,
        @Schema(description = "상담 제목") String title,
        @Schema(description = "상담 신청 시각") LocalDateTime requestedAt,
        @Schema(description = "상담 완료 시각") LocalDateTime counseledAt,
        @Schema(description = "최근 수정 시각") LocalDateTime updatedAt
) {

    public static CounselingRecordSummaryResponseDTO from(CounselingRecord record) {
        return new CounselingRecordSummaryResponseDTO(
                record.getId(),
                record.getStudent().getId(),
                record.getStudent().getUser().getName(),
                record.getCounselingMethod(),
                record.getStatus(),
                record.getTitle(),
                record.getCounselingMethod() == CounselingMethod.ONLINE ? record.getCreatedAt() : null,
                record.getCounseledAt(),
                record.getUpdatedAt()
        );
    }
}
