package com.msa4lmsv2academic.domain.graduation.response;

import com.msa4lmsv2academic.domain.graduation.service.GraduationCreditDiagnosisResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전공·교양·필수·선택 학점 진단 응답")
public record CreditDiagnosisResponseDTO(
        @Schema(description = "진단 대상 학생 ID", example = "1001")
        Long studentId,
        @Schema(description = "취득한 전공 학점", example = "54")
        int earnedMajorCredits,
        @Schema(description = "취득한 교양 학점", example = "32")
        int earnedGeneralCredits,
        @Schema(description = "취득한 필수 학점", example = "40")
        int earnedRequiredCredits,
        @Schema(description = "취득한 선택 학점", example = "80")
        int earnedElectiveCredits,
        @Schema(description = "중복을 제거한 총 취득 학점", example = "120")
        int earnedTotalCredits,
        @Schema(description = "부족한 전공 학점", example = "6")
        int shortageMajorCredits,
        @Schema(description = "부족한 교양 학점", example = "0")
        int shortageGeneralCredits,
        @Schema(description = "부족한 총 학점", example = "10")
        int shortageTotalCredits,
        @Schema(description = "ERD에 정의된 전공·교양·총학점 요건 충족 여부", example = "false")
        boolean satisfied
) {

    public static CreditDiagnosisResponseDTO from(GraduationCreditDiagnosisResult result) {
        return new CreditDiagnosisResponseDTO(
                result.studentId(),
                result.earnedMajorCredits(),
                result.earnedGeneralCredits(),
                result.earnedRequiredCredits(),
                result.earnedElectiveCredits(),
                result.earnedTotalCredits(),
                result.shortageMajorCredits(),
                result.shortageGeneralCredits(),
                result.shortageTotalCredits(),
                result.satisfied()
        );
    }
}
