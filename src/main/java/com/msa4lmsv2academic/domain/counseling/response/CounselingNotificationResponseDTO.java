package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상담 변경·취소 알림")
public record CounselingNotificationResponseDTO(
        @Schema(description = "알림 ID", example = "501") Long notificationId,
        @Schema(description = "상담 예약 ID", example = "101") Long appointmentId,
        @Schema(description = "알림 종류", example = "APPOINTMENT_CANCELLED") CounselingNotificationType type,
        @Schema(description = "변경 전 상담 상태", example = "PENDING") CounselingAppointmentStatus previousStatus,
        @Schema(description = "변경 후 상담 상태", example = "CANCELLED") CounselingAppointmentStatus newStatus,
        @Schema(description = "알림 메시지", example = "학생이 상담 예약을 취소했습니다.") String message,
        @Schema(description = "상담 예정 시각", example = "2026-09-04T14:00:00") LocalDateTime appointmentAt,
        @Schema(description = "상담 주제", example = "진로 및 취업 상담") String topic,
        @Schema(description = "학생 이름", example = "김민준") String studentName,
        @Schema(description = "교수 이름", example = "박현빈") String professorName,
        @Schema(description = "읽음 여부", example = "false") boolean read,
        @Schema(description = "읽은 시각", nullable = true) LocalDateTime readAt,
        @Schema(description = "알림 생성 시각") LocalDateTime createdAt
) {
    public static CounselingNotificationResponseDTO from(CounselingNotification notification) {
        var appointment = notification.getAppointment();
        return new CounselingNotificationResponseDTO(
                notification.getId(),
                appointment.getId(),
                notification.getType(),
                notification.getPreviousStatus(),
                notification.getNewStatus(),
                notification.getMessage(),
                appointment.getAppointmentAt(),
                appointment.getTopic(),
                appointment.getStudent().getUser().getName(),
                appointment.getProfessor().getUser().getName(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
