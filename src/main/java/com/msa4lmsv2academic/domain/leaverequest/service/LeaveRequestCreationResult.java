package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;

public record LeaveRequestCreationResult(LeaveRequestResponseDTO response, boolean created) { }
