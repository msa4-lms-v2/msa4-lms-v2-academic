package com.msa4lmsv2academic.domain.outbox.response;

import com.msa4lmsv2academic.domain.student.entity.Student;

// Payment의 학생 증명서(재학/졸업증명서) 발급이 필요로 하는 학적·졸업요건 정보를
// 한 번의 내부 호출로 내려준다. SnapshotSyncController와 같은 인증 없는 시스템 경로로만 노출한다.
public record StudentCertificateEligibilityResponseDTO(
        Long studentId,
        String studentNumber,
        String name,
        String departmentName,
        String collegeName,
        byte gradeLevel,
        short admissionYear,
        String academicStatus,
        Boolean graduationSatisfied,
        Integer earnedTotalCredits
) {
    public static StudentCertificateEligibilityResponseDTO of(
            Student student,
            Boolean graduationSatisfied,
            Integer earnedTotalCredits
    ) {
        return new StudentCertificateEligibilityResponseDTO(
                student.getId(),
                student.getStudentNumber(),
                student.getUser().getName(),
                student.getDepartment().getName(),
                student.getDepartment().getCollege() == null ? null : student.getDepartment().getCollege().getName(),
                student.getGradeLevel(),
                student.getAdmissionYear(),
                student.getAcademicStatus().name(),
                graduationSatisfied,
                earnedTotalCredits
        );
    }
}
