package com.msa4lmsv2academic.domain.academicschedule.request;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "학사일정 등록 요청")
public record AcademicScheduleCreateRequestDTO(
        @Schema(description = "일정 제목", example = "2026학년도 2학기 수강신청", minLength = 1,
                maxLength = 100, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "일정 상세 내용. 공백이면 null로 저장됩니다.",
                example = "수강신청 기간과 유의사항을 확인해 주세요.", maxLength = 5000, nullable = true)
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        String content,

        @Schema(description = "일정 시작일", example = "2026-08-17", format = "date",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "startDate는 필수입니다.")
        LocalDate startDate,

        @Schema(description = "일정 종료일. 하루 일정이면 생략합니다.", example = "2026-08-21",
                format = "date", nullable = true)
        LocalDate endDate,

        @Schema(description = "일정 공개 대상 역할", example = "STUDENT",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "targetRole은 필수입니다.")
        AcademicScheduleTargetRole targetRole
) {
}
