package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureQueryResult;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "교수 담당 강의")
public record ProfessorLectureResponseDTO(
        @Schema(description = "강의 ID", example = "101") Long classId,
        @Schema(description = "교과목 ID", example = "31") Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "학점", example = "3") Byte credits,
        @Schema(description = "대상 학년", example = "3", nullable = true) Byte targetGrade,
        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED") CompletionType completionType,
        @Schema(description = "개설 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "담당 교수 ID", example = "12") Long professorId,
        @Schema(description = "담당 교수명", example = "홍길동") String professorName,
        @Schema(description = "학기 ID", example = "5") Long semesterId,
        @Schema(description = "학년도", example = "2026") Short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "분반", example = "01") String sectionNo,
        @Schema(description = "강의실", example = "공학관 301호", nullable = true) String classroom,
        @Schema(description = "정원", example = "40") Integer capacity,
        @Schema(description = "강의 상태", example = "OPEN") LectureStatus status,
        @Schema(description = "중간고사 반영 비율", example = "30") Integer midtermRatio,
        @Schema(description = "기말고사 반영 비율", example = "30") Integer finalRatio,
        @Schema(description = "과제 반영 비율", example = "30") Integer assignmentRatio,
        @Schema(description = "출석 반영 비율", example = "10") Integer attendanceRatio,
        @Schema(description = "강의계획서 본문", nullable = true) String syllabus,
        @Schema(description = "현재 활성 수강 인원", example = "32") Long currentEnrollmentCount,
        @Schema(description = "강의 시간표") List<ProfessorLectureScheduleResponseDTO> schedules
) {

    public static ProfessorLectureResponseDTO from(ProfessorLectureQueryResult result) {
        return new ProfessorLectureResponseDTO(
                result.classId(),
                result.courseId(),
                result.courseCode(),
                result.courseName(),
                result.credits(),
                result.targetGrade(),
                result.completionType(),
                result.departmentName(),
                result.professorId(),
                result.professorName(),
                result.semesterId(),
                result.academicYear(),
                result.term(),
                result.sectionNo(),
                result.classroom(),
                result.capacity(),
                result.status(),
                result.midtermRatio(),
                result.finalRatio(),
                result.assignmentRatio(),
                result.attendanceRatio(),
                result.syllabus(),
                result.currentEnrollmentCount(),
                result.schedules().stream()
                        .map(ProfessorLectureScheduleResponseDTO::from)
                        .toList()
        );
    }
}
