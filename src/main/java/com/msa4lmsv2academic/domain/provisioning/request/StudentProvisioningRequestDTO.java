package com.msa4lmsv2academic.domain.provisioning.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

// auth에서 받아온 정보
public record StudentProvisioningRequestDTO(
        @NotNull
        @Positive
        Long userId,

        @NotBlank
        String name,

        @NotNull
        @Past
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate,

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
        this(userId, name, null, email, phoneNumber, address, departmentId, admissionYear, null, null);
    }

    public StudentProvisioningRequestDTO(Long userId, String name, String email, String phoneNumber,
                                         String address, Long departmentId, Short admissionYear,
                                         Long admissionCandidateId) {
        this(userId, name, null, email, phoneNumber, address, departmentId, admissionYear,
                admissionCandidateId, null);
    }

    public StudentProvisioningRequestDTO(Long userId, String name, String email, String phoneNumber,
                                         String address, Long departmentId, Short admissionYear,
                                         Long admissionCandidateId, Long advisorProfessorId) {
        this(userId, name, null, email, phoneNumber, address, departmentId, admissionYear,
                admissionCandidateId, advisorProfessorId);
    }
}
