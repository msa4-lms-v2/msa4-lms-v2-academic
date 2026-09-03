package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestQueryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "공결 신청 처리 상태 조회 결과")
public record ExcuseRequestStatusResponseDTO(
        @Schema(description = "공결 신청 ID", example = "301")
        Long id,

        @Schema(description = "수강신청 ID", example = "12001")
        Long enrollmentId,

        @Schema(description = "Academic 학생 ID", example = "2001")
        Long studentId,

        @Schema(description = "학생 사용자 ID", example = "10001")
        Long studentUserId,

        @Schema(description = "학생 이름", example = "김미래")
        String studentName,

        @Schema(description = "강의 ID", example = "501")
        Long classId,

        @Schema(description = "교과목 ID", example = "101")
        Long courseId,

        @Schema(description = "교과목 코드", example = "CSE301")
        String courseCode,

        @Schema(description = "교과목명", example = "운영체제")
        String courseName,

        @Schema(description = "분반", example = "01")
        String sectionNo,

        @Schema(description = "Academic 교수 ID", example = "3001")
        Long professorId,

        @Schema(description = "교수 사용자 ID", example = "11001")
        Long professorUserId,

        @Schema(description = "담당 교수 이름", example = "홍길동")
        String professorName,

        @Schema(description = "공결 대상 수업일", example = "2026-09-01")
        LocalDate lectureDate,

        @Schema(description = "공결 대상 교시", example = "2")
        Byte period,

        @Schema(description = "공결 신청 사유", example = "질병으로 병원 진료를 받았습니다.")
        String reason,

        @Schema(description = "처리 상태", example = "PENDING")
        ExcuseRequestStatus status,

        @Schema(description = "반려 사유. 반려되지 않았다면 null입니다.", example = "증빙 자료가 불충분합니다.")
        String rejectReason,

        @Schema(description = "첨부파일 원본 이름. 첨부가 없다면 null입니다.", example = "진료확인서.pdf")
        String attachmentOriginalName,

        @Schema(description = "첨부파일 MIME 타입. 첨부가 없다면 null입니다.", example = "application/pdf")
        String attachmentContentType,

        @Schema(description = "첨부파일 크기(byte). 첨부가 없다면 null입니다.", example = "2048")
        Long attachmentSize,

        @Schema(description = "신청 시각", example = "2026-09-02T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "마지막 변경 시각", example = "2026-09-02T14:10:00")
        LocalDateTime updatedAt
) {

    public static ExcuseRequestStatusResponseDTO from(ExcuseRequestQueryResult result) {
        return new ExcuseRequestStatusResponseDTO(
                result.id(),
                result.enrollmentId(),
                result.studentId(),
                result.studentUserId(),
                result.studentName(),
                result.classId(),
                result.courseId(),
                result.courseCode(),
                result.courseName(),
                result.sectionNo(),
                result.professorId(),
                result.professorUserId(),
                result.professorName(),
                result.lectureDate(),
                result.period(),
                result.reason(),
                result.status(),
                result.rejectReason(),
                result.attachmentOriginalName(),
                result.attachmentContentType(),
                result.attachmentSize(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
