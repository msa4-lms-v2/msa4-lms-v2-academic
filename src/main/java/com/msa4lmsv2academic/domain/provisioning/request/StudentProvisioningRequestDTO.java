package com.msa4lmsv2academic.domain.provisioning.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// auth에서 받아온 정보
public record StudentProvisioningRequestDTO(
        @NotNull
        @Positive
        Long userId,

        @NotBlank
        String name,

        @NotBlank
        @Email
        String email,

        String phoneNumber,

        String address,

        @NotNull
        @Positive
        Long departmentId,

        @NotNull
        Short admissionYear,
        Long admissionCandidateId,
        Long advisorProfessorId
) {
    public StudentProvisioningRequestDTO(Long userId, String name, String email, String phoneNumber,
                                         String address, Long departmentId, Short admissionYear) {
        this(userId, name, email, phoneNumber, address, departmentId, admissionYear, null, null);
    }

    public StudentProvisioningRequestDTO(Long userId, String name, String email, String phoneNumber,
                                         String address, Long departmentId, Short admissionYear,
                                         Long admissionCandidateId) {
        this(userId, name, email, phoneNumber, address, departmentId, admissionYear,
                admissionCandidateId, null);
    }
}
