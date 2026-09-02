package com.msa4lmsv2academic.domain.student.response;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "권한 범위 내 학생 학적 요약")
public record StudentSummaryResponseDTO(
        @Schema(description = "Student 엔티티 ID", example = "10") Long studentId,
        @Schema(description = "Auth accountId와 동일한 Academic 사용자 ID", example = "25") Long userId,
        @Schema(description = "학생 이름", example = "김학생") String name,
        @Schema(description = "소속 학과 ID", example = "3") Long departmentId,
        @Schema(description = "소속 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "복수전공 학과 ID. 복수전공이 없는 경우 null", example = "12", nullable = true)
        Long doubleMajorId,
        @Schema(description = "복수전공 학과명. 복수전공이 없는 경우 null", example = "경영학과", nullable = true)
        String doubleMajorName,
        @Schema(description = "현재 학년", example = "2") byte gradeLevel,
        @Schema(description = "입학 연도", example = "2025") short admissionYear,
        @Schema(description = "현재 학적 상태", example = "ENROLLED") AcademicStatus academicStatus,
        @Schema(description = "지도교수의 Professor 엔티티 ID. 미배정이면 null", example = "8", nullable = true)
        Long advisorProfessorId,
        @Schema(description = "지도교수명. 미배정이면 null", example = "박교수", nullable = true)
        String advisorName
) {

    public static StudentSummaryResponseDTO from(Student student) {
        return new StudentSummaryResponseDTO(
                student.getId(),
                student.getUser().getId(),
                student.getUser().getName(),
                student.getDepartment().getId(),
                student.getDepartment().getName(),
                student.getDoubleMajor() == null ? null : student.getDoubleMajor().getId(),
                student.getDoubleMajor() == null ? null : student.getDoubleMajor().getName(),
                student.getGradeLevel(),
                student.getAdmissionYear(),
                student.getAcademicStatus(),
                student.getAdvisor() == null ? null : student.getAdvisor().getId(),
                student.getAdvisor() == null ? null : student.getAdvisor().getUser().getName()
        );
    }
}
