package com.msa4lmsv2academic.domain.professor.response;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 교수 목록 항목")
public record ProfessorSummaryResponseDTO(
        @Schema(description = "Professor 엔티티 ID", example = "10") Long professorId,
        @Schema(description = "Auth accountId와 동일한 Academic 사용자 ID", example = "25") Long userId,
        @Schema(description = "교수 이름", example = "김교수") String name,
        @Schema(description = "교수 이메일", example = "professor@example.com", nullable = true) String email,
        @Schema(description = "Auth에서 동기화된 읽기 전용 계정 상태", example = "ACTIVE") UserStatus status,
        @Schema(description = "소속 학과 ID", example = "3") Long departmentId,
        @Schema(description = "소속 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "임용 연도", example = "2020", nullable = true) Short hireYear
) {

    public static ProfessorSummaryResponseDTO from(Professor professor) {
        return new ProfessorSummaryResponseDTO(
                professor.getId(),
                professor.getUser().getId(),
                professor.getUser().getName(),
                professor.getUser().getEmail(),
                professor.getUser().getStatus(),
                professor.getDepartment().getId(),
                professor.getDepartment().getName(),
                professor.getHireYear()
        );
    }
}
