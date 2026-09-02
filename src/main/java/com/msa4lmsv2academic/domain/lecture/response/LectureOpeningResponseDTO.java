package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequest;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "강의 개설 신청 처리 결과")
public record LectureOpeningResponseDTO(
        @Schema(description = "강의 개설 신청 ID", example = "101")
        Long openingRequestId,
        @Schema(description = "낙관적 잠금 버전", example = "0")
        Long version,
        @Schema(description = "교과목 ID", example = "31")
        Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301")
        String courseCode,
        @Schema(description = "교과목명", example = "운영체제")
        String courseName,
        @Schema(description = "담당 교수 ID", example = "12")
        Long professorId,
        @Schema(description = "담당 교수명", example = "홍길동")
        String professorName,
        @Schema(description = "학기 ID", example = "5")
        Long semesterId,
        @Schema(description = "학년도", example = "2026")
        Short academicYear,
        @Schema(description = "학기", example = "FIRST")
        SemesterTerm term,
        @Schema(description = "분반", example = "01")
        String sectionNo,
        @Schema(description = "신청 정원", example = "40")
        Integer requestedCapacity,
        @Schema(description = "강의실", example = "공학관 301호")
        String classroom,
        @Schema(description = "중간고사 반영 비율", example = "30")
        Integer midtermRatio,
        @Schema(description = "기말고사 반영 비율", example = "30")
        Integer finalRatio,
        @Schema(description = "과제 반영 비율", example = "30")
        Integer assignmentRatio,
        @Schema(description = "출석 반영 비율", example = "10")
        Integer attendanceRatio,
        @Schema(description = "강의계획서 본문")
        String syllabus,
        @Schema(description = "강의 시간표")
        List<LectureOpeningScheduleResponseDTO> schedules,
        @Schema(description = "처리 상태", example = "PENDING")
        LectureOpeningRequestStatus status,
        @Schema(description = "반려 사유", nullable = true)
        String rejectReason,
        @Schema(description = "검토 관리자 사용자 ID", nullable = true, example = "3")
        Long reviewedBy,
        @Schema(description = "검토 시각", nullable = true)
        LocalDateTime reviewedAt,
        @Schema(description = "승인 후 생성된 강의 ID", nullable = true, example = "88")
        Long lectureId,
        @Schema(description = "신청 시각")
        LocalDateTime createdAt,
        @Schema(description = "최종 수정 시각")
        LocalDateTime updatedAt
) {

    public static LectureOpeningResponseDTO from(LectureOpeningRequest request) {
        return new LectureOpeningResponseDTO(
                request.getId(),
                request.getVersion(),
                request.getCourse().getId(),
                request.getCourse().getCode(),
                request.getCourse().getName(),
                request.getProfessor().getId(),
                request.getProfessor().getUser().getName(),
                request.getSemester().getId(),
                request.getSemester().getAcademicYear(),
                request.getSemester().getTerm(),
                request.getSectionNo(),
                request.getRequestedCapacity(),
                request.getClassroom(),
                request.getMidtermRatio(),
                request.getFinalRatio(),
                request.getAssignmentRatio(),
                request.getAttendanceRatio(),
                request.getSyllabus(),
                request.getSchedules().stream()
                        .map(LectureOpeningScheduleResponseDTO::from)
                        .toList(),
                request.getStatus(),
                request.getRejectReason(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getReviewedAt(),
                request.getApprovedLecture() == null ? null : request.getApprovedLecture().getId(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
