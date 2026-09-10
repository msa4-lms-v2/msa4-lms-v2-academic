package com.msa4lmsv2academic.domain.notification.event;

import com.msa4lmsv2academic.domain.notification.response.NotificationResponseDTO;

public record NotificationCreatedEvent(Long recipientUserId, NotificationResponseDTO payload) { }
