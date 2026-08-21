package com.msa4lmsv2academic.domain.graduation.response;

import com.msa4lmsv2academic.domain.graduation.entity.CreditDiagnosisStatus;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역할 범위별 학생 학점 진단 현황")
public record CreditDiagnosisSummaryResponseDTO(
        @Schema(description = "학생 ID", example = "1001") Long studentId,
        @Schema(description = "학생 이름", example = "김학생") String studentName,
        @Schema(description = "학과 ID", example = "1") Long departmentId,
        @Schema(description = "학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "입학연도", example = "2024") short admissionYear,
        @Schema(description = "학적 상태", example = "ENROLLED") AcademicStatus academicStatus,
        @Schema(description = "최소 전공학점. 기준 미설정이면 null", example = "60", nullable = true)
        Integer requiredMajorCredits,
        @Schema(description = "최소 교양학점. 기준 미설정이면 null", example = "30", nullable = true)
        Integer requiredGeneralCredits,
        @Schema(description = "최소 총학점. 기준 미설정이면 null", example = "130", nullable = true)
        Integer requiredTotalCredits,
        @Schema(description = "취득 전공학점", example = "54") int earnedMajorCredits,
        @Schema(description = "취득 교양학점", example = "32") int earnedGeneralCredits,
        @Schema(description = "취득 필수학점", example = "40") int earnedRequiredCredits,
        @Schema(description = "취득 선택학점", example = "46") int earnedElectiveCredits,
        @Schema(description = "중복 제거 총 취득학점", example = "86") int earnedTotalCredits,
        @Schema(description = "부족 전공학점. 기준 미설정이면 null", example = "6", nullable = true)
        Integer shortageMajorCredits,
        @Schema(description = "부족 교양학점. 기준 미설정이면 null", example = "0", nullable = true)
        Integer shortageGeneralCredits,
        @Schema(description = "부족 총학점. 기준 미설정이면 null", example = "44", nullable = true)
        Integer shortageTotalCredits,
        @Schema(description = "진단 상태", example = "NOT_SATISFIED") CreditDiagnosisStatus diagnosisStatus,
        @Schema(description = "진단 상태 사유", example = "전공 6학점, 총 44학점이 부족합니다.") String reason
) {
}
