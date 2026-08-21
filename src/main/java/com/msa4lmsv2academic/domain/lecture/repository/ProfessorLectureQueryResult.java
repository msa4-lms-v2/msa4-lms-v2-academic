package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.util.List;

public record ProfessorLectureQueryResult(
        Long classId,
        Long courseId,
        String courseCode,
        String courseName,
        Byte credits,
        Byte targetGrade,
        CompletionType completionType,
        String departmentName,
        Long professorId,
        String professorName,
        Long semesterId,
        Short academicYear,
        SemesterTerm term,
        String sectionNo,
        String classroom,
        Integer capacity,
        LectureStatus status,
        Integer midtermRatio,
        Integer finalRatio,
        Integer assignmentRatio,
        Integer attendanceRatio,
        String syllabus,
        Long currentEnrollmentCount,
        List<ProfessorLectureScheduleQueryResult> schedules
) {
}
