package com.msa4lmsv2academic.domain.grade.response;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import java.util.List;

public record GradeClassResponseDTO(
        Long classId,
        String courseCode,
        String courseName,
        String sectionNo,
        int midtermRatio,
        int finalRatio,
        int assignmentRatio,
        int attendanceRatio,
        int enrollmentCount,
        com.msa4lmsv2academic.domain.gradeperiod.service.GradeOperationPeriodService.EntryWindow entryWindow,
        List<GradeItemResponseDTO> grades
) {
    public static GradeClassResponseDTO from(Lecture lecture, List<Enrollment> enrollments) {
        return from(lecture, enrollments, null);
    }

    public static GradeClassResponseDTO from(Lecture lecture, List<Enrollment> enrollments,
            com.msa4lmsv2academic.domain.gradeperiod.service.GradeOperationPeriodService.EntryWindow entryWindow) {
        return new GradeClassResponseDTO(
                lecture.getId(),
                lecture.getCourse().getCode(),
                lecture.getCourse().getName(),
                lecture.getSectionNo(),
                lecture.getMidtermRatio(),
                lecture.getFinalRatio(),
                lecture.getAssignmentRatio(),
                lecture.getAttendanceRatio(),
                enrollments.size(),
                entryWindow,
                enrollments.stream().map(GradeItemResponseDTO::from).toList()
        );
    }
}
