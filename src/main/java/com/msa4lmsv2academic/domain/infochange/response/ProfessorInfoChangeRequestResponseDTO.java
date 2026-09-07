package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "교수 프로필 변경 신청")
public record ProfessorInfoChangeRequestResponseDTO(
        @Schema(description = "신청 ID", example = "1") Long id,
        @Schema(description = "Professor 엔티티 ID", example = "10") Long professorId,
        @Schema(description = "신청자 이름", example = "김교수") String professorName,
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
        @Schema(description = "증빙 PDF/JPEG/PNG/GIF/WebP/HWP/HWPX 목록. 목록 응답에서는 null", nullable = true)
        List<ProfessorInfoChangeRequestFileResponseDTO> files
) {
    public static ProfessorInfoChangeRequestResponseDTO summary(ProfessorInfoChangeRequest request) {
        return from(request, null, null);
    }

    public static ProfessorInfoChangeRequestResponseDTO detail(
            ProfessorInfoChangeRequest request,
            String newProfileImageUrl,
            List<ProfessorInfoChangeRequestFileResponseDTO> files
    ) {
        return from(request, newProfileImageUrl, files);
    }

    private static ProfessorInfoChangeRequestResponseDTO from(
            ProfessorInfoChangeRequest request,
            String newProfileImageUrl,
            List<ProfessorInfoChangeRequestFileResponseDTO> files
    ) {
        return new ProfessorInfoChangeRequestResponseDTO(
                request.getId(),
                request.getProfessor().getId(),
                request.getProfessor().getUser().getName(),
                request.getProfessor().getDepartment().getId(),
                request.getProfessor().getDepartment().getName(),
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
                files
        );
    }
}
