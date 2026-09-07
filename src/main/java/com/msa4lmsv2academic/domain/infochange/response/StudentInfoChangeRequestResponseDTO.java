package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "학생 프로필 변경 신청")
public record StudentInfoChangeRequestResponseDTO(
        @Schema(description = "신청 ID", example = "1") Long id,
        @Schema(description = "Student 엔티티 ID", example = "7") Long studentId,
        @Schema(description = "신청자 이름", example = "김학생") String studentName,
        @Schema(description = "신청자 학과 ID", example = "3") Long departmentId,
        @Schema(description = "신청자 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "변경할 이름", nullable = true) String newName,
        @Schema(description = "변경할 전화번호", nullable = true) String newPhoneNumber,
        @Schema(description = "변경할 이메일", nullable = true) String newEmail,
        @Schema(description = "변경할 주소", nullable = true) String newAddress,
        @Schema(description = "변경할 프로필 이미지 임시 URL", nullable = true) String newProfileImageUrl,
        @Schema(description = "신청 사유") String reason,
        @Schema(description = "신청 상태", allowableValues = {"REQUESTED", "APPROVED", "REJECTED", "CANCELLED"})
        InfoChangeRequestStatus status,
        @Schema(description = "관리자 사용자 ID", nullable = true) Long reviewedBy,
        @Schema(description = "관리자 처리 시각", nullable = true) LocalDateTime reviewedAt,
        @Schema(description = "반려 사유", nullable = true) String rejectReason,
        @Schema(description = "본인 취소 시각", nullable = true) LocalDateTime cancelledAt,
        @Schema(description = "생성 시각") LocalDateTime createdAt,
        @Schema(description = "최종 변경 시각") LocalDateTime updatedAt,
        @Schema(
                description = "변경 신청 항목",
                allowableValues = {"NAME", "PHONE_NUMBER", "EMAIL", "ADDRESS", "PROFILE_IMAGE"}
        )
        List<String> changedFields,
        @Schema(description = "증빙 첨부파일 수", example = "2") long attachmentCount,
        @Schema(description = "증빙 PDF/JPEG/PNG/GIF/WebP/HWP/HWPX 목록. 목록 응답에서는 null", nullable = true)
        List<StudentInfoChangeRequestFileResponseDTO> files
) {
    public static StudentInfoChangeRequestResponseDTO summary(
            StudentInfoChangeRequest request,
            long attachmentCount
    ) {
        return from(request, null, attachmentCount, null);
    }

    public static StudentInfoChangeRequestResponseDTO detail(
            StudentInfoChangeRequest request,
            String newProfileImageUrl,
            List<StudentInfoChangeRequestFileResponseDTO> files
    ) {
        return from(request, newProfileImageUrl, files.size(), files);
    }

    private static StudentInfoChangeRequestResponseDTO from(
            StudentInfoChangeRequest request,
            String newProfileImageUrl,
            long attachmentCount,
            List<StudentInfoChangeRequestFileResponseDTO> files
    ) {
        return new StudentInfoChangeRequestResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
                request.getStudent().getDepartment().getId(),
                request.getStudent().getDepartment().getName(),
                request.getNewName(),
                request.getNewPhoneNumber(),
                request.getNewEmail(),
                request.getNewAddress(),
                newProfileImageUrl,
                request.getReason(),
                request.getStatus(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getReviewedAt(),
                request.getRejectReason(),
                request.getCancelledAt(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                changedFields(request),
                attachmentCount,
                files
        );
    }

    private static List<String> changedFields(StudentInfoChangeRequest request) {
        ArrayList<String> fields = new ArrayList<>();
        if (request.getNewName() != null) fields.add("NAME");
        if (request.getNewPhoneNumber() != null) fields.add("PHONE_NUMBER");
        if (request.getNewEmail() != null) fields.add("EMAIL");
        if (request.getNewAddress() != null) fields.add("ADDRESS");
        if (request.getNewProfileImageKey() != null) fields.add("PROFILE_IMAGE");
        return List.copyOf(fields);
    }
}
