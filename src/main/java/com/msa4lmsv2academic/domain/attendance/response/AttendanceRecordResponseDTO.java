package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRecordQueryResult;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "출결 기록")
public record AttendanceRecordResponseDTO(
        @Schema(description = "출결 기록 ID", example = "501") Long id,
        @Schema(description = "수강 ID", example = "12001") Long enrollmentId,
        @Schema(description = "학생 ID", example = "2001") Long studentId,
        @Schema(description = "학생 이름", example = "김미래") String studentName,
        @Schema(description = "개설 강의 ID", example = "101") Long classId,
        @Schema(description = "과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "과목명", example = "운영체제") String courseName,
        @Schema(description = "분반", example = "01") String sectionNo,
        @Schema(description = "담당 교수 이름", example = "홍길동") String professorName,
        @Schema(description = "학년도", example = "2026") Short academicYear,
        @Schema(description = "학기", example = "SECOND") SemesterTerm term,
        @Schema(description = "출석 세션 ID", example = "701") Long sessionId,
        @Schema(description = "수업일", example = "2026-09-07") LocalDate lectureDate,
        @Schema(description = "교시", example = "2") Integer period,
        @Schema(description = "출결 상태", example = "PRESENT") AttendanceStatus status,
        @Schema(description = "출결 비고", example = "교통 지연 확인") String remarks,
        @Schema(description = "QR 체크인 시각", example = "2026-09-07T10:02:00") LocalDateTime checkInTime,
        @Schema(description = "수동 수정 여부", example = "true") Boolean modified,
        @Schema(description = "마지막 변경 시각", example = "2026-09-07T11:00:00") LocalDateTime updatedAt
) {

    public static AttendanceRecordResponseDTO from(AttendanceRecordQueryResult result) {
        return new AttendanceRecordResponseDTO(
                result.id(), result.enrollmentId(), result.studentId(), result.studentName(),
                result.classId(), result.courseCode(), result.courseName(), result.sectionNo(),
                result.professorName(), result.academicYear(), result.term(), result.sessionId(),
                result.lectureDate(), result.period(), result.status(), result.remarks(),
                result.checkInTime(), result.modified(), result.updatedAt()
        );
    }

    public static AttendanceRecordResponseDTO from(Attendance attendance) {
        Enrollment enrollment = attendance.getEnrollment();
        Lecture lecture = enrollment.getLecture();
        Semester semester = lecture.getSemester();
        return new AttendanceRecordResponseDTO(
                attendance.getId(),
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getUser().getName(),
                lecture.getId(),
                lecture.getCourse().getCode(),
                lecture.getCourse().getName(),
                lecture.getSectionNo(),
                lecture.getProfessor().getUser().getName(),
                semester.getAcademicYear(),
                semester.getTerm(),
                attendance.getSession() == null ? null : attendance.getSession().getId(),
                attendance.getLectureDate(),
                attendance.getPeriod(),
                attendance.getStatus(),
                attendance.getRemarks(),
                attendance.getCheckInTime(),
                attendance.isModified(),
                attendance.getUpdatedAt()
        );
    }
}
