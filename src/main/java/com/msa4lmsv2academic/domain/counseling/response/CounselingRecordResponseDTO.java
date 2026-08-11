package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "상담 기록 상세")
public record CounselingRecordResponseDTO(
        @Schema(description = "상담 기록 ID") Long id,
        @Schema(description = "학생 ID") Long studentId,
        @Schema(description = "학생 이름") String studentName,
        @Schema(description = "교수 ID") Long professorId,
        @Schema(description = "교수 이름") String professorName,
        @Schema(description = "상담 방식") CounselingMethod counselingMethod,
        @Schema(description = "상담 상태") CounselingStatus status,
        @Schema(description = "상담 제목") String title,
        @Schema(description = "학생이 작성한 온라인 상담 내용") String studentContent,
        @Schema(description = "교수 답변 또는 대면 상담 결과") String professorResponse,
        @Schema(description = "온라인 상담 신청 시각") LocalDateTime requestedAt,
        @Schema(description = "상담 완료 시각") LocalDateTime counseledAt,
        @Schema(description = "교수 답변 시각") LocalDateTime respondedAt,
        @Schema(description = "최근 수정 시각") LocalDateTime updatedAt
) {

    public static CounselingRecordResponseDTO from(CounselingRecord record) {
        return new CounselingRecordResponseDTO(
                record.getId(),
                record.getStudent().getId(),
                record.getStudent().getUser().getName(),
                record.getProfessor().getId(),
                record.getProfessor().getUser().getName(),
                record.getCounselingMethod(),
                record.getStatus(),
                record.getTitle(),
                record.getStudentContent(),
                record.getProfessorResponse(),
                record.getCounselingMethod() == CounselingMethod.ONLINE ? record.getCreatedAt() : null,
                record.getCounseledAt(),
                record.getRespondedAt(),
                record.getUpdatedAt()
        );
    }
}
