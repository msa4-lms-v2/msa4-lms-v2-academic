package com.msa4lmsv2academic.domain.admission.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "입학 예정자 등록 요청")
public record AdmissionCandidateCreateRequestDTO(
        @Schema(description = "입학 예정자 이름", example = "김민수", minLength = 1, maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 50, message = "name은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "생년월일", example = "2008-03-15", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "birthDate는 필수입니다.")
        @Past(message = "birthDate는 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @Schema(description = "계정 생성에 사용할 이메일", example = "minsu@example.com", requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100)
        @NotBlank(message = "email은 필수입니다.")
        @Email(message = "email 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "email은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "전화번호. 생략 가능", example = "010-1234-5678", nullable = true,
                maxLength = 20)
        @Size(max = 20, message = "phoneNumber는 20자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "주소. 생략 가능", example = "서울특별시 중구", nullable = true,
                maxLength = 255)
        @Size(max = 255, message = "address는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "입학 예정 학과 ID. 활성 학과만 허용", example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "departmentId는 필수입니다.")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "배정할 지도교수 ID. 입학 예정 학과의 활성 교수만 허용", example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "advisorProfessorId는 필수입니다.")
        @Positive(message = "advisorProfessorId는 양수여야 합니다.")
        Long advisorProfessorId,

        @Schema(description = "입학 예정 연도. 서버 기준 현재 연도 또는 다음 연도", example = "2027",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "admissionYear는 필수입니다.")
        @Positive(message = "admissionYear는 양수여야 합니다.")
        Integer admissionYear
) {
}
