package com.msa4lmsv2academic.domain.grade.response;

import com.msa4lmsv2academic.domain.grade.entity.GradeCorrectionHistory;
import java.time.LocalDateTime;

public record GradeCorrectionHistoryResponseDTO(
        Long historyId,
        Long enrollmentId,
        Long studentId,
        String studentName,
        String fieldChanged,
        String previousValue,
        String newValue,
        Long changedBy,
        String changedByName,
        String reason,
        LocalDateTime createdAt
) {
    public static GradeCorrectionHistoryResponseDTO from(GradeCorrectionHistory history) {
        return new GradeCorrectionHistoryResponseDTO(
                history.getId(),
                history.getEnrollment().getId(),
                history.getEnrollment().getStudent().getId(),
                history.getEnrollment().getStudent().getUser().getName(),
                history.getFieldChanged(),
                history.getPreviousValue(),
                history.getNewValue(),
                history.getChangedBy().getId(),
                history.getChangedBy().getName(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}
