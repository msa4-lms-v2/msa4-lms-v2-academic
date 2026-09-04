package com.msa4lmsv2academic.domain.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    MANUAL_REVIEW_REQUIRED
}
