package com.msa4lmsv2academic.domain.admission.response;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "입학 예정자 상세 정보")
public record AdmissionCandidateDetailResponseDTO(
        @Schema(description = "입학 예정자 ID", example = "15") Long id,
        @Schema(description = "수정 충돌 감지 버전", example = "0") Long version,
        @Schema(description = "수험번호 또는 지원번호", example = "APP-2027-00015") String applicationNumber,
        @Schema(description = "이름", example = "김민수") String name,
        @Schema(description = "생년월일", example = "2008-03-15") LocalDate birthDate,
        @Schema(description = "이메일", example = "minsu@example.com", nullable = true) String email,
        @Schema(description = "전화번호", example = "010-1234-5678", nullable = true) String phoneNumber,
        @Schema(description = "주소", example = "서울특별시 중구", nullable = true) String address,
        @Schema(description = "학과 ID", example = "1") Long departmentId,
        @Schema(description = "학과 코드", example = "001") String departmentCode,
        @Schema(description = "학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "입학 예정 연도", example = "2027") short admissionYear,
        @Schema(description = "등록 상태", example = "REGISTERED") AdmissionCandidateStatus status,
        @Schema(description = "프로비저닝 완료 뒤 연결된 Academic 학생 ID", example = "20270001",
                nullable = true) Long studentId,
        @Schema(description = "최초 등록 관리자 사용자 ID", example = "3") Long createdBy,
        @Schema(description = "최근 상태 변경 관리자 사용자 ID. 시스템 변경이면 null", example = "3",
                nullable = true) Long statusChangedBy,
        @Schema(description = "최근 상태 변경 시각", example = "2026-08-21T16:00:00",
                nullable = true) LocalDateTime statusChangedAt,
        @Schema(description = "등록 시각", example = "2026-08-21T15:00:00") LocalDateTime createdAt,
        @Schema(description = "최종 수정 시각", example = "2026-08-21T16:00:00") LocalDateTime updatedAt
) {

    public static AdmissionCandidateDetailResponseDTO from(AdmissionCandidate candidate) {
        return new AdmissionCandidateDetailResponseDTO(
                candidate.getId(),
                candidate.getVersion(),
                candidate.getApplicationNumber(),
                candidate.getName(),
                candidate.getBirthDate(),
                candidate.getEmail(),
                candidate.getPhoneNumber(),
                candidate.getAddress(),
                candidate.getDepartment().getId(),
                candidate.getDepartment().getCode(),
                candidate.getDepartment().getName(),
                candidate.getAdmissionYear(),
                candidate.getStatus(),
                candidate.getStudent() == null ? null : candidate.getStudent().getId(),
                candidate.getCreatedBy().getId(),
                candidate.getStatusChangedBy() == null ? null : candidate.getStatusChangedBy().getId(),
                candidate.getStatusChangedAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
