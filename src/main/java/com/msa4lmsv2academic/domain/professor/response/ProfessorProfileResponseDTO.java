package com.msa4lmsv2academic.domain.professor.response;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "교수 본인 프로필")
public record ProfessorProfileResponseDTO(
        @Schema(description = "Professor 엔티티 ID", example = "10") Long professorId,
        @Schema(description = "Auth accountId와 동일한 Academic 사용자 ID", example = "25") Long userId,
        @Schema(description = "이름", example = "김교수") String name,
        @Schema(description = "이메일", example = "professor@example.com", nullable = true) String email,
        @Schema(description = "연락처", example = "010-1234-5678", nullable = true) String phoneNumber,
        @Schema(description = "주소", example = "서울특별시 중구", nullable = true) String address,
        @Schema(description = "프로필 이미지 임시 URL(발급 후 1일 유효)", nullable = true) String profileImageUrl,
        @Schema(description = "Auth에서 동기화된 읽기 전용 계정 상태", example = "ACTIVE") UserStatus status,
        @Schema(description = "소속 단과대학명", example = "공과대학", nullable = true) String collegeName,
        @Schema(description = "소속 학과 ID", example = "3") Long departmentId,
        @Schema(description = "소속 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "임용 연도", example = "2020", nullable = true) Short hireYear
) {
    public static ProfessorProfileResponseDTO from(Professor professor, String profileImageUrl) {
        return new ProfessorProfileResponseDTO(
                professor.getId(),
                professor.getUser().getId(),
                professor.getUser().getName(),
                professor.getUser().getEmail(),
                professor.getUser().getPhoneNumber(),
                professor.getUser().getAddress(),
                profileImageUrl,
                professor.getUser().getStatus(),
                professor.getDepartment().getCollege() == null ? null : professor.getDepartment().getCollege().getName(),
                professor.getDepartment().getId(),
                professor.getDepartment().getName(),
                professor.getHireYear()
        );
    }
}
