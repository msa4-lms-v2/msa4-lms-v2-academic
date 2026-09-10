package com.msa4lmsv2academic.domain.notification.response;

import com.msa4lmsv2academic.domain.notification.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공통 알림")
public record NotificationResponseDTO(
        @Schema(description = "알림 ID", example = "501") Long notificationId,
        @Schema(description = "알림 범주", example = "ACADEMIC") NotificationCategory category,
        @Schema(description = "구체 알림 유형", example = "LEAVE_ACTION_REQUIRED") NotificationType type,
        @Schema(description = "연결 원본 유형", example = "LEAVE_REQUEST") NotificationResourceType resourceType,
        @Schema(description = "연결 원본 ID", example = "101") Long resourceId,
        @Schema(description = "알림 제목") String title,
        @Schema(description = "알림 메시지") String message,
        @Schema(description = "읽음 여부") boolean read,
        @Schema(description = "읽은 시각", nullable = true) LocalDateTime readAt,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
    public static NotificationResponseDTO from(Notification notification) {
        return new NotificationResponseDTO(notification.getId(), notification.getCategory(), notification.getType(),
                notification.getResourceType(), notification.getResourceId(), notification.getTitle(), notification.getMessage(),
                notification.isRead(), notification.getReadAt(), notification.getCreatedAt());
    }
}
