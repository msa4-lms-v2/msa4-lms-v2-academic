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

        @Positive
        Long majorId,

        @NotNull
        Short admissionYear
) {
}
