package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequestFile;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProfessorInfoChangeRequestFileResponseDTO(
        @Schema(description = "첨부파일 ID") Long id,
        @Schema(description = "원본 파일명") String fileName,
        @Schema(description = "MIME 타입", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(byte)", example = "102400") long fileSize,
        @Schema(description = "임시 다운로드 URL(발급 후 1일 유효)") String downloadUrl
) {
    public static ProfessorInfoChangeRequestFileResponseDTO from(
            ProfessorInfoChangeRequestFile file,
            String downloadUrl
    ) {
        return new ProfessorInfoChangeRequestFileResponseDTO(
                file.getId(), file.getFileName(), file.getContentType(), file.getFileSize(), downloadUrl
        );
    }
}
