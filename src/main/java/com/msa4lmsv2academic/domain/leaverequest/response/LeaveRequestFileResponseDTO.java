package com.msa4lmsv2academic.domain.leaverequest.response;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record LeaveRequestFileResponseDTO(
        @Schema(description = "증빙 파일 ID", example = "1") Long id,
        @Schema(description = "원본 파일명", example = "진단서.pdf") String originalName,
        @Schema(description = "MIME 타입", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(byte), 최대 10MB", maximum = "10485760", example = "245760") long size,
        @Schema(description = "업로드 시각(KST)", example = "2026-12-01T10:30:00") LocalDateTime createdAt
) {
    public static LeaveRequestFileResponseDTO from(LeaveRequestFile file) {
        return new LeaveRequestFileResponseDTO(file.getId(), file.getOriginalName(), file.getContentType(),
                file.getSize(), file.getCreatedAt());
    }
}
