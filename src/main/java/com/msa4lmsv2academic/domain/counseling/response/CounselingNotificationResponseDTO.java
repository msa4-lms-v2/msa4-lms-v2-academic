package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "온라인 상담 알림")
public record CounselingNotificationResponseDTO(
        @Schema(description = "알림 ID", example = "501") Long notificationId,
        @Schema(description = "상담 ID", example = "101") Long counselingId,
        @Schema(description = "알림 종류", example = "COUNSELING_ANSWERED") CounselingNotificationType type,
        @Schema(description = "변경 전 상담 상태", nullable = true) CounselingStatus previousStatus,
        @Schema(description = "변경 후 상담 상태", example = "ANSWERED") CounselingStatus newStatus,
        @Schema(description = "알림 메시지") String message,
        @Schema(description = "상담 제목", example = "진로 및 취업 상담") String title,
        @Schema(description = "학생 이름", example = "김민준") String studentName,
        @Schema(description = "교수 이름", example = "박현빈") String professorName,
        @Schema(description = "읽음 여부", example = "false") boolean read,
        @Schema(description = "읽은 시각", nullable = true) LocalDateTime readAt,
        @Schema(description = "알림 생성 시각") LocalDateTime createdAt
) {
    public static CounselingNotificationResponseDTO from(CounselingNotification notification) {
        var counseling = notification.getCounseling();
        return new CounselingNotificationResponseDTO(
                notification.getId(),
                counseling.getId(),
                notification.getType(),
                notification.getPreviousStatus(),
                notification.getNewStatus(),
                notification.getMessage(),
                counseling.getTitle(),
                counseling.getStudent().getUser().getName(),
                counseling.getProfessor().getUser().getName(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
