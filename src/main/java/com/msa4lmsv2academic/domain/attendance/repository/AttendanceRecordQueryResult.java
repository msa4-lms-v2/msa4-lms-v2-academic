package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecordQueryResult(
        Long id,
        Long enrollmentId,
        Long studentId,
        String studentName,
        Long classId,
        String courseCode,
        String courseName,
        String sectionNo,
        String professorName,
        Short academicYear,
        SemesterTerm term,
        Long sessionId,
        LocalDate lectureDate,
        Integer period,
        AttendanceStatus status,
        String remarks,
        LocalDateTime checkInTime,
        Boolean modified,
        LocalDateTime updatedAt
) {
}
