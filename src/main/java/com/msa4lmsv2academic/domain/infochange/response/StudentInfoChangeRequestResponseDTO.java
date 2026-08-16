package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import java.time.LocalDateTime;
import java.util.List;

public record StudentInfoChangeRequestResponseDTO(
        Long id,
        Long studentId,
        String studentName,
        String newName,
        String newPhoneNumber,
        String newEmail,
        String newAddress,
        String newProfileImageUrl,
        String reason,
        InfoChangeRequestStatus status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String rejectReason,
        LocalDateTime createdAt,
        List<StudentInfoChangeRequestFileResponseDTO> files
) {
    // 목록 조회용. 첨부파일 다운로드 URL은 MinIO 서명 발급 호출이 필요해 상세 조회에서만 채운다.
    public static StudentInfoChangeRequestResponseDTO summary(StudentInfoChangeRequest request) {
        return new StudentInfoChangeRequestResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
                request.getNewName(),
                request.getNewPhoneNumber(),
                request.getNewEmail(),
                request.getNewAddress(),
                null,
                request.getReason(),
                request.getStatus(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getReviewedAt(),
                request.getRejectReason(),
                request.getCreatedAt(),
                null
        );
    }

    public static StudentInfoChangeRequestResponseDTO detail(
            StudentInfoChangeRequest request,
            String newProfileImageUrl,
            List<StudentInfoChangeRequestFileResponseDTO> files
    ) {
        return new StudentInfoChangeRequestResponseDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getStudent().getUser().getName(),
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
                request.getCreatedAt(),
                files
        );
    }
}
