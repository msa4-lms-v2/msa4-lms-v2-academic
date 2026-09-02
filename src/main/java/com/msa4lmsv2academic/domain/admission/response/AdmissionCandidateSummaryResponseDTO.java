package com.msa4lmsv2academic.domain.admission.response;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "입학 예정자 목록 항목. 생년월일·연락처·주소는 포함하지 않습니다.")
public record AdmissionCandidateSummaryResponseDTO(
        @Schema(description = "입학 예정자 ID", example = "15") Long id,
        @Schema(description = "수험번호 또는 지원번호", example = "APP-2027-00015") String applicationNumber,
        @Schema(description = "이름", example = "김민수") String name,
        @Schema(description = "학과 ID", example = "1") Long departmentId,
        @Schema(description = "학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "입학 예정 연도", example = "2027") short admissionYear,
        @Schema(description = "등록 상태", example = "REGISTERED") AdmissionCandidateStatus status,
        @Schema(description = "등록 시각", example = "2026-08-21T15:00:00") LocalDateTime createdAt
) {

    public static AdmissionCandidateSummaryResponseDTO from(AdmissionCandidate candidate) {
        return new AdmissionCandidateSummaryResponseDTO(
                candidate.getId(),
                candidate.getApplicationNumber(),
                candidate.getName(),
                candidate.getDepartment().getId(),
                candidate.getDepartment().getName(),
                candidate.getAdmissionYear(),
                candidate.getStatus(),
                candidate.getCreatedAt()
        );
    }
}
