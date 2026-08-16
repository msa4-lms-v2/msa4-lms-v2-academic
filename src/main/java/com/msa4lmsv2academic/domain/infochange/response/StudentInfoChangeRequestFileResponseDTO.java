package com.msa4lmsv2academic.domain.infochange.response;

import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequestFile;
import io.swagger.v3.oas.annotations.media.Schema;

public record StudentInfoChangeRequestFileResponseDTO(
        @Schema(description = "첨부파일 ID") Long id,
        @Schema(description = "원본 파일명") String fileName,
        @Schema(description = "임시 다운로드 URL(발급 후 1일 유효)") String downloadUrl
) {
    public static StudentInfoChangeRequestFileResponseDTO from(StudentInfoChangeRequestFile file, String downloadUrl) {
        return new StudentInfoChangeRequestFileResponseDTO(file.getId(), file.getFileName(), downloadUrl);
    }
}
