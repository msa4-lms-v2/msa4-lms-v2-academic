package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;

public record DepartmentTransferCreationResult(DepartmentTransferResponseDTO response, boolean created) { }
