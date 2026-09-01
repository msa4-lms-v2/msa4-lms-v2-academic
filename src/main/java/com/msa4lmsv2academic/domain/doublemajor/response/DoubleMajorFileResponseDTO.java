package com.msa4lmsv2academic.domain.doublemajor.response;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestFile;
import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DoubleMajorFileResponseDTO(
        @Schema(description = "서류 ID", example = "1") Long id,
        @Schema(description = "서류 유형", example = "SELF_INTRODUCTION") TransferDocumentType documentType,
        @Schema(description = "원본 파일명", example = "self-introduction.pdf") String originalName,
        @Schema(description = "MIME 유형", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(byte)", example = "164331") long size,
        @Schema(description = "등록 시각(KST)", example = "2026-12-01T10:30:00") LocalDateTime createdAt
) {
    public static DoubleMajorFileResponseDTO from(AcademicChangeRequestFile file) {
        return new DoubleMajorFileResponseDTO(
                file.getId(),
                file.getDocumentType(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                file.getCreatedAt());
    }
}
