package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "대면 상담 기록 등록 요청")
public record InPersonCounselingCreateRequestDTO(
        @Schema(description = "상담한 담당 학생 ID", example = "1001")
        @NotNull(message = "studentId는 필수입니다.")
        @Positive(message = "studentId는 양수여야 합니다.")
        Long studentId,

        @Schema(description = "상담 제목", example = "복수전공 진행 상담")
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 150, message = "title은 150자 이하여야 합니다.")
        String title,

        @Schema(description = "교수가 작성한 상담 내용과 결과", example = "다음 학기 복수전공 신청 일정을 안내했습니다.")
        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        String content,

        @Schema(description = "실제 대면 상담 일시", example = "2026-08-10T14:00:00")
        @NotNull(message = "counseledAt은 필수입니다.")
        @PastOrPresent(message = "counseledAt은 미래일 수 없습니다.")
        LocalDateTime counseledAt
) {
}
