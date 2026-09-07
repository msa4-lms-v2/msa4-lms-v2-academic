package com.msa4lmsv2academic.domain.grade.response;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import java.math.BigDecimal;

public record GradeItemResponseDTO(
        Long enrollmentId,
        Long studentId,
        String studentName,
        BigDecimal midtermScore,
        BigDecimal finalScore,
        BigDecimal assignmentScore,
        BigDecimal attendanceScore,
        BigDecimal totalScore,
        String letterGrade,
        GradeStatus gradeStatus
) {
    public static GradeItemResponseDTO from(Enrollment enrollment) {
        return new GradeItemResponseDTO(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getUser().getName(),
                enrollment.getMidtermScore(),
                enrollment.getFinalScore(),
                enrollment.getAssignmentScore(),
                enrollment.getAttendanceScore(),
                enrollment.getTotalScore(),
                enrollment.getLetterGrade(),
                enrollment.getGradeStatus()
        );
    }
}
