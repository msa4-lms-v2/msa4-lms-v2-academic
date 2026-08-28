package com.msa4lmsv2academic.domain.leaverequest.service;

public record LeaveAttachment(String originalName, String storedName, String contentType, Long size) {
    public static LeaveAttachment empty() {
        return new LeaveAttachment(null, null, null, null);
    }
}
