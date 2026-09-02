package com.msa4lmsv2academic.domain.student.response;

import com.msa4lmsv2academic.domain.student.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentProfileResponseDTO(
        @Schema(description = "이름", example = "김학생") String name,
        @Schema(description = "이메일", example = "student@example.com", nullable = true) String email,
        @Schema(description = "연락처", example = "010-1234-5678", nullable = true) String phoneNumber,
        @Schema(description = "주소", example = "서울특별시 중구", nullable = true) String address,
        @Schema(description = "프로필 이미지 임시 URL(발급 후 1일 유효)", nullable = true) String profileImageUrl,
        @Schema(description = "소속 단과대학명", example = "공과대학", nullable = true) String collegeName,
        @Schema(description = "학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "학년", example = "3") byte gradeLevel,
        @Schema(description = "입학년도", example = "2024") short admissionYear,
        @Schema(description = "학적 상태", example = "ENROLLED") String academicStatus,
        @Schema(description = "지도교수명", example = "김교수", nullable = true) String advisorName,
        @Schema(description = "총 취득 학점", example = "72") int totalCredits
) {
    public static StudentProfileResponseDTO from(Student student, int totalCredits, String profileImageUrl) {
        return new StudentProfileResponseDTO(
                student.getUser().getName(),
                student.getUser().getEmail(),
                student.getUser().getPhoneNumber(),
                student.getUser().getAddress(),
                profileImageUrl,
                student.getDepartment().getCollege() != null ? student.getDepartment().getCollege().getName() : null,
                student.getDepartment().getName(),
                student.getGradeLevel(),
                student.getAdmissionYear(),
                student.getAcademicStatus().name(),
                student.getAdvisor() != null ? student.getAdvisor().getUser().getName() : null,
                totalCredits
        );
    }
}
