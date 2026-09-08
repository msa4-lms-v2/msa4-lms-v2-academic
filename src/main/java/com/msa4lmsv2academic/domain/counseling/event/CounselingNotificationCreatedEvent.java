package com.msa4lmsv2academic.domain.counseling.event;

import com.msa4lmsv2academic.domain.counseling.response.CounselingNotificationResponseDTO;

public record CounselingNotificationCreatedEvent(
        Long recipientUserId,
        CounselingNotificationResponseDTO payload
) {
}
