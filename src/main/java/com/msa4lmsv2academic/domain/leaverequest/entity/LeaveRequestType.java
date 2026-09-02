package com.msa4lmsv2academic.domain.leaverequest.entity;

public enum LeaveRequestType {
    GENERAL_LEAVE, GENERAL_RETURN, MILITARY_LEAVE, MILITARY_RETURN;

    public boolean isLeave() {
        return this == GENERAL_LEAVE || this == MILITARY_LEAVE;
    }
}
