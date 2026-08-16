package com.msa4lmsv2academic.domain.student.response;

import com.msa4lmsv2academic.domain.student.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentProfileResponseDTO(
        @Schema(description = "이름") String name,
        @Schema(description = "이메일") String email,
        @Schema(description = "연락처") String phoneNumber,
        @Schema(description = "주소") String address,
        @Schema(description = "소속 단과대학명") String collegeName,
        @Schema(description = "학과명") String departmentName,
        @Schema(description = "전공명") String majorName,
        @Schema(description = "학년") byte gradeLevel,
        @Schema(description = "입학년도") short admissionYear,
        @Schema(description = "학적 상태") String academicStatus,
        @Schema(description = "지도교수명") String advisorName,
        @Schema(description = "총 취득 학점") int totalCredits
) {
    public static StudentProfileResponseDTO from(Student student, int totalCredits) {
        return new StudentProfileResponseDTO(
                student.getUser().getName(),
                student.getUser().getEmail(),
                student.getUser().getPhoneNumber(),
                student.getUser().getAddress(),
                student.getDepartment().getCollege() != null ? student.getDepartment().getCollege().getName() : null,
                student.getDepartment().getName(),
                student.getMajor() != null ? student.getMajor().getName() : null,
                student.getGradeLevel(),
                student.getAdmissionYear(),
                student.getAcademicStatus().name(),
                student.getAdvisor() != null ? student.getAdvisor().getUser().getName() : null,
                totalCredits
        );
    }
}
