package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartItemQueryResult;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "학생 본인 수강 장바구니 항목")
public record EnrollmentCartItemResponseDTO(
        @Schema(description = "장바구니 항목 ID", example = "301") Long cartItemId,
        @Schema(description = "장바구니 추가 시각", example = "2026-08-27T10:30:00") LocalDateTime createdAt,
        @Schema(description = "개설 강의 ID", example = "101") Long lectureId,
        @Schema(description = "교과목 ID", example = "31") Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "학점", example = "3") Byte credits,
        @Schema(description = "대상 학년", example = "3", nullable = true) Byte targetGrade,
        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED") CompletionType completionType,
        @Schema(description = "개설 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "담당 교수명", example = "홍길동") String professorName,
        @Schema(description = "학기 ID", example = "11") Long semesterId,
        @Schema(description = "학년도", example = "2026") Short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "분반", example = "01") String sectionNo,
        @Schema(description = "강의실", example = "공학관 301호", nullable = true) String classroom,
        @Schema(description = "정원", example = "40") Integer capacity,
        @Schema(description = "강의 상태", example = "OPEN") LectureStatus lectureStatus,
        @Schema(description = "예상 시간표에 표시할 강의 시간 목록") List<EnrollmentCartScheduleResponseDTO> schedules
) {

    public static EnrollmentCartItemResponseDTO from(EnrollmentCartItemQueryResult result) {
        return new EnrollmentCartItemResponseDTO(
                result.cartItemId(),
                result.createdAt(),
                result.lectureId(),
                result.courseId(),
                result.courseCode(),
                result.courseName(),
                result.credits(),
                result.targetGrade(),
                result.completionType(),
                result.departmentName(),
                result.professorName(),
                result.semesterId(),
                result.academicYear(),
                result.term(),
                result.sectionNo(),
                result.classroom(),
                result.capacity(),
                result.lectureStatus(),
                result.schedules().stream().map(EnrollmentCartScheduleResponseDTO::from).toList()
        );
    }
}
