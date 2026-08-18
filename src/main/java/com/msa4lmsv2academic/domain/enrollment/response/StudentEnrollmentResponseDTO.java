package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentEnrollmentQueryResult;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "학생 본인 수강 강의")
public record StudentEnrollmentResponseDTO(
        @Schema(description = "수강신청 ID", example = "501") Long enrollmentId,
        @Schema(description = "수강 상태", example = "ACTIVE") EnrollmentStatus enrollmentStatus,
        @Schema(description = "수강신청 일시", example = "2026-02-10T10:30:00") LocalDateTime enrolledAt,
        @Schema(description = "강의 ID", example = "101") Long classId,
        @Schema(description = "교과목 ID", example = "31") Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "학점", example = "3") Byte credits,
        @Schema(description = "대상 학년", example = "3", nullable = true) Byte targetGrade,
        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED") CompletionType completionType,
        @Schema(description = "개설 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "담당 교수명", example = "홍길동") String professorName,
        @Schema(description = "학년도", example = "2026") Short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "분반", example = "01") String sectionNo,
        @Schema(description = "강의실", example = "공학관 301호", nullable = true) String classroom,
        @Schema(description = "정원", example = "40") Integer capacity,
        @Schema(description = "강의 상태", example = "OPEN") LectureStatus status
) {

    public static StudentEnrollmentResponseDTO from(StudentEnrollmentQueryResult result) {
        return new StudentEnrollmentResponseDTO(
                result.enrollmentId(),
                result.enrollmentStatus(),
                result.enrolledAt(),
                result.classId(),
                result.courseId(),
                result.courseCode(),
                result.courseName(),
                result.credits(),
                result.targetGrade(),
                result.completionType(),
                result.departmentName(),
                result.professorName(),
                result.academicYear(),
                result.term(),
                result.sectionNo(),
                result.classroom(),
                result.capacity(),
                result.status()
        );
    }
}
