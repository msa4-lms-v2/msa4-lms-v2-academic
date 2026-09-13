package com.msa4lmsv2academic.domain.outbox.response;

import com.msa4lmsv2academic.domain.professor.entity.Professor;

// Payment의 교수 재직증명서 발급이 필요로 하는 교원 정보를 내려준다.
// SnapshotSyncController와 같은 인증 없는 시스템 경로로만 노출한다.
public record ProfessorCertificateEligibilityResponseDTO(
        Long professorId,
        String professorNumber,
        String name,
        String departmentName,
        String collegeName,
        Short hireYear,
        String status
) {
    public static ProfessorCertificateEligibilityResponseDTO from(Professor professor) {
        return new ProfessorCertificateEligibilityResponseDTO(
                professor.getId(),
                professor.getProfessorNumber(),
                professor.getUser().getName(),
                professor.getDepartment().getName(),
                professor.getDepartment().getCollege() == null ? null : professor.getDepartment().getCollege().getName(),
                professor.getHireYear(),
                professor.getUser().getStatus().name()
        );
    }
}
