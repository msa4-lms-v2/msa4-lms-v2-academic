package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExcuseRequestQueryResult(
        Long id,
        Long enrollmentId,
        Long studentId,
        Long studentUserId,
        String studentName,
        Long classId,
        Long courseId,
        String courseCode,
        String courseName,
        String sectionNo,
        Long professorId,
        Long professorUserId,
        String professorName,
        LocalDate lectureDate,
        Byte period,
        String reason,
        ExcuseRequestStatus status,
        String rejectReason,
        String attachmentOriginalName,
        String attachmentContentType,
        Long attachmentSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
