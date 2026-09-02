package com.msa4lmsv2academic.domain.transfer.response;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestFile;
import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DepartmentTransferFileResponseDTO(
        @Schema(description = "서류 ID", example = "1") Long id,
        @Schema(description = "필수 서류 유형", example = "SELF_INTRODUCTION") TransferDocumentType documentType,
        @Schema(description = "원본 파일명", example = "자기소개서.pdf") String originalName,
        @Schema(description = "MIME 타입", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(byte), 최대 10MB", example = "245760") long size,
        @Schema(description = "업로드 시각(KST)", example = "2026-12-01T10:30:00") LocalDateTime createdAt
) {
    public static DepartmentTransferFileResponseDTO from(AcademicChangeRequestFile file) {
        return new DepartmentTransferFileResponseDTO(file.getId(), file.getDocumentType(), file.getOriginalName(),
                file.getContentType(), file.getSize(), file.getCreatedAt());
    }
}
