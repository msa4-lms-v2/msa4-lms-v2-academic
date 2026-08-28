package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableEntryQueryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "학생 시간표 강의 항목")
public record StudentTimetableEntryResponseDTO(
        @Schema(description = "수강신청 ID", example = "501") Long enrollmentId,
        @Schema(description = "개설 강의 ID", example = "101") Long lectureId,
        @Schema(description = "교과목 ID", example = "31") Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "학점", example = "3") Byte credits,
        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED") CompletionType completionType,
        @Schema(description = "담당 교수명", example = "홍길동") String professorName,
        @Schema(description = "분반", example = "01") String sectionNo,
        @Schema(description = "강의실", example = "공학관 301호", nullable = true) String classroom,
        @Schema(description = "시간표에 표시할 요일·교시 목록") List<StudentTimetableScheduleResponseDTO> schedules
) {

    public static StudentTimetableEntryResponseDTO from(StudentTimetableEntryQueryResult result) {
        return new StudentTimetableEntryResponseDTO(
                result.enrollmentId(),
                result.lectureId(),
                result.courseId(),
                result.courseCode(),
                result.courseName(),
                result.credits(),
                result.completionType(),
                result.professorName(),
                result.sectionNo(),
                result.classroom(),
                result.schedules().stream().map(StudentTimetableScheduleResponseDTO::from).toList()
        );
    }
}
