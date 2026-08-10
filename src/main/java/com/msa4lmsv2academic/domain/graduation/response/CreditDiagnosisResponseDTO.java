package com.msa4lmsv2academic.domain.graduation.response;

import com.msa4lmsv2academic.domain.graduation.entity.CreditCategory;
import com.msa4lmsv2academic.domain.graduation.repository.EarnedCreditSummaryData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "전공·교양·필수·선택 학점 진단 응답")
public record CreditDiagnosisResponseDTO(
        @Schema(description = "진단 대상 학생 ID", example = "1001")
        Long studentId,
        @Schema(description = "중복을 제거한 총 취득 학점", example = "120")
        int earnedTotalCredits,
        @Schema(description = "전공·교양·필수·선택 구분별 취득 학점")
        Map<CreditCategory, Integer> earnedCreditsByCategory,
        @Schema(description = "부족한 전공 학점", example = "6")
        int shortageMajorCredits,
        @Schema(description = "부족한 교양 학점", example = "0")
        int shortageGeneralCredits,
        @Schema(description = "부족한 총 학점", example = "10")
        int shortageTotalCredits,
        @Schema(description = "ERD에 정의된 전공·교양·총학점 요건 충족 여부", example = "false")
        boolean satisfied
) {

    public CreditDiagnosisResponseDTO {
        earnedCreditsByCategory = Map.copyOf(earnedCreditsByCategory);
    }

    public static CreditDiagnosisResponseDTO from(
            Long studentId,
            EarnedCreditSummaryData earnedCredits,
            int shortageMajorCredits,
            int shortageGeneralCredits,
            int shortageTotalCredits
    ) {
        return new CreditDiagnosisResponseDTO(
                studentId,
                earnedCredits.totalCredits(),
                earnedCredits.creditsByCategory(),
                shortageMajorCredits,
                shortageGeneralCredits,
                shortageTotalCredits,
                shortageMajorCredits == 0 && shortageGeneralCredits == 0 && shortageTotalCredits == 0
        );
    }
}
