package com.msa4lmsv2academic.domain.evaluation.response;

import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "강의평가 제출 결과")
public record LectureEvaluationResponseDTO(
        @Schema(description = "강의평가 ID", example = "41")
        Long evaluationId,
        @Schema(description = "수강 내역 ID", example = "301")
        Long enrollmentId,
        @Schema(description = "강의 ID", example = "21")
        Long lectureId,
        @Schema(description = "제출 일시", example = "2026-06-10T14:30:00", format = "date-time")
        LocalDateTime submittedAt
) {

    public static LectureEvaluationResponseDTO from(LectureEvaluation evaluation) {
        return new LectureEvaluationResponseDTO(
                evaluation.getId(),
                evaluation.getEnrollment().getId(),
                evaluation.getEnrollment().getLecture().getId(),
                evaluation.getSubmittedAt()
        );
    }
}
