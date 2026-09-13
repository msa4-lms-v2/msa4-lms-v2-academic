package com.msa4lmsv2academic.domain.leaverequest.response;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequest;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record LeaveRequestResponseDTO(
        @Schema(description = "신청 ID", example = "1") Long id,
        @Schema(description = "Academic 학생 ID", example = "1") Long studentId,
        @Schema(description = "학생 이름", example = "김학생") String studentName,
        @Schema(description = "학번", nullable = true, example = "20241234") String studentNumber,
        @Schema(description = "소속 학과", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "학년", example = "3") byte gradeLevel,
        @Schema(description = "현재 학적 상태", example = "ENROLLED") AcademicStatus academicStatus,
        @Schema(description = "학생 이메일", nullable = true, example = "student@mirae.ac.kr") String studentEmail,
        @Schema(description = "학생 연락처", nullable = true, example = "010-1234-5678") String studentPhoneNumber,
        @Schema(description = "서버에서 확정한 신청 유형", example = "GENERAL_LEAVE") LeaveRequestType requestType,
        @Schema(description = "신청 사유", maxLength = 500, example = "개인 사정") String reason,
        @Schema(description = "적용 학년도", example = "2027", minimum = "1", maximum = "32767") short targetYear,
        @Schema(description = "적용 학기", example = "1", minimum = "1", maximum = "2") byte targetSemester,
        @Schema(description = "휴학의 복학 예정 학년도. 복학 신청은 null", example = "2028", nullable = true) Short returnYear,
        @Schema(description = "휴학의 복학 예정 학기. 복학 신청은 null", example = "1", nullable = true) Byte returnSemester,
        @Schema(description = "처리 상태", example = "PENDING") LeaveRequestStatus status,
        @Schema(description = "지도교수 검토자 이름", nullable = true, example = "김교수") String advisorReviewerName,
        @Schema(description = "지도교수 검토 시각(KST)", nullable = true, example = "2026-12-10T11:00:00") LocalDateTime advisorReviewedAt,
        @Schema(description = "지도교수 반려 사유. 교수 반려 시에만 존재", maxLength = 500, nullable = true, example = "신청 내용을 보완해 주세요.") String advisorRejectReason,
        @Schema(description = "반려 사유", maxLength = 500, nullable = true, example = "신청 내용을 확인해주세요.") String rejectReason,
        @Schema(description = "직접 또는 자퇴 승인에 따른 취소 사유", maxLength = 500, nullable = true, example = "자퇴 최종 승인으로 자동 취소되었습니다.") String cancelReason,
        @Schema(description = "첫 번째 증빙의 원본 파일명", maxLength = 255, nullable = true, example = "증빙.hwp") String attachmentOriginalName,
        @Schema(description = "첨부 MIME 타입", nullable = true, example = "application/pdf") String attachmentContentType,
        @Schema(description = "첨부 크기(byte), 최대 10485760", maximum = "10485760", nullable = true, example = "1024") Long attachmentSize,
        @Schema(description = "PDF/HWP/HWPX/JPEG/PNG/GIF/WebP 증빙 목록. 최대 5개") List<LeaveRequestFileResponseDTO> files,
        @Schema(description = "신청 시각(KST)", example = "2026-12-10T10:00:00") LocalDateTime createdAt,
        @Schema(description = "최종 변경 시각(KST). 승인·취소의 전용 시각이 아님", example = "2026-12-10T10:00:00") LocalDateTime updatedAt
) {
    public static LeaveRequestResponseDTO from(LeaveRequest request) {
        List<LeaveRequestFileResponseDTO> files = request.getFiles().stream()
                .map(LeaveRequestFileResponseDTO::from)
                .toList();
        var first = request.getFiles().isEmpty() ? null : request.getFiles().getFirst();
        Long firstSize = first == null ? request.getAttachmentSize() : Long.valueOf(first.getSize());
        return new LeaveRequestResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
                request.getStudent().getStudentNumber(),
                request.getStudent().getDepartment().getName(),
                request.getStudent().getGradeLevel(),
                request.getStudent().getAcademicStatus(),
                request.getStudent().getUser().getEmail(),
                request.getStudent().getUser().getPhoneNumber(),
                request.getRequestType(),
                request.getReason(),
                request.getTargetYear(),
                request.getTargetSemester(),
                request.getReturnYear(),
                request.getReturnSemester(),
                request.getStatus(),
                request.getAdvisorReviewedBy() == null ? null : request.getAdvisorReviewedBy().getName(),
                request.getAdvisorReviewedAt(),
                request.getAdvisorRejectReason(),
                request.getRejectReason(),
                request.getCancelReason(),
                first == null ? request.getAttachmentOriginalName() : first.getOriginalName(),
                first == null ? request.getAttachmentContentType() : first.getContentType(),
                firstSize,
                files,
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
