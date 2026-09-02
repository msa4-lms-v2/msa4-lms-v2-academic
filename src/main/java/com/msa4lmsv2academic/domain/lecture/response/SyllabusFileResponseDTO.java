package com.msa4lmsv2academic.domain.lecture.response;

import com.msa4lmsv2academic.domain.lecture.entity.SyllabusFile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "강의계획서 파일 정보")
public record SyllabusFileResponseDTO(
        @Schema(description = "파일 ID", example = "501")
        Long fileId,

        @Schema(description = "강의 ID", example = "101")
        Long classId,

        @Schema(description = "원본 파일명", example = "운영체제_강의계획서.pdf")
        String originalName,

        @Schema(description = "파일 MIME 타입", example = "application/pdf")
        String contentType,

        @Schema(description = "파일 크기(byte)", example = "248320")
        long size,

        @Schema(description = "업로드한 사용자 ID", example = "2")
        Long uploadedBy,

        @Schema(description = "업로드 일시")
        LocalDateTime createdAt
) {

    public static SyllabusFileResponseDTO from(SyllabusFile syllabusFile) {
        return new SyllabusFileResponseDTO(
                syllabusFile.getId(),
                syllabusFile.getLecture().getId(),
                syllabusFile.getOriginalName(),
                syllabusFile.getContentType(),
                syllabusFile.getSize(),
                syllabusFile.getUploadedBy().getId(),
                syllabusFile.getCreatedAt()
        );
    }
}
