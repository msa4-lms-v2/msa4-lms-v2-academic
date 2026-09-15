package com.msa4lmsv2academic.domain.provisioning.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

// auth에서 받아온 정보
public record ProfessorProvisioningRequestDTO(
        @NotNull
        @Positive
        Long userId,

        @NotBlank String name,

        @NotNull @Past
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate,

        @NotBlank @Email String email,

        String phoneNumber,

        String address,

        @NotNull  @Positive  Long departmentId,

        @NotNull Short hireYear
) {
    public ProfessorProvisioningRequestDTO(Long userId, String name, String email, String phoneNumber,
                                            String address, Long departmentId, Short hireYear) {
        this(userId, name, null, email, phoneNumber, address, departmentId, hireYear);
    }
}
