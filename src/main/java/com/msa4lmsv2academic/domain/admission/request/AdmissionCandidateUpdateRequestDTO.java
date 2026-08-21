package com.msa4lmsv2academic.domain.admission.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "입학 예정자 인적사항·학과·입학연도 부분 수정 요청")
public record AdmissionCandidateUpdateRequestDTO(
        @Schema(description = "변경할 이름. 생략 또는 null이면 유지", example = "김민수", maxLength = 50)
        @Size(max = 50, message = "name은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 생년월일. 생략 또는 null이면 유지", example = "2008-03-15",
                format = "date")
        @Past(message = "birthDate는 과거 날짜여야 합니다.")
        LocalDate birthDate,

        @Schema(description = "변경할 이메일. 생략 또는 null이면 유지하고 공백 문자열이면 삭제",
                example = "minsu@example.com", nullable = true, maxLength = 100)
        @Email(message = "email 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "email은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "변경할 전화번호. 생략 또는 null이면 유지하고 공백 문자열이면 삭제",
                example = "010-1234-5678", nullable = true, maxLength = 20)
        @Size(max = 20, message = "phoneNumber는 20자 이하여야 합니다.")
        String phoneNumber,

        @Schema(description = "변경할 주소. 생략 또는 null이면 유지하고 공백 문자열이면 삭제",
                example = "서울특별시 중구", nullable = true, maxLength = 255)
        @Size(max = 255, message = "address는 255자 이하여야 합니다.")
        String address,

        @Schema(description = "변경할 활성 학과 ID. 생략 또는 null이면 유지", example = "2")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "변경할 입학 예정 연도. 생략 또는 null이면 유지", example = "2027")
        @Positive(message = "admissionYear는 양수여야 합니다.")
        Integer admissionYear
) {

    public boolean hasAnyUpdateField() {
        return name != null || birthDate != null || email != null || phoneNumber != null
                || address != null || departmentId != null || admissionYear != null;
    }
}
