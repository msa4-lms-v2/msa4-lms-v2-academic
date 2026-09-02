package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공결 증빙 파일 정보")
public record ExcuseAttachmentResponseDTO(
        @Schema(description = "공결 신청 ID", example = "31")
        Long requestId,

        @Schema(description = "원본 파일명", example = "진료확인서.pdf")
        String originalName,

        @Schema(description = "파일 MIME 타입", example = "application/pdf")
        String contentType,

        @Schema(description = "파일 크기(byte)", example = "245760")
        Long size,

        @Schema(description = "증빙 정보 최종 수정 시각", example = "2026-09-02T10:30:00")
        LocalDateTime updatedAt
) {

    public static ExcuseAttachmentResponseDTO from(ExcuseRequest excuseRequest) {
        return new ExcuseAttachmentResponseDTO(
                excuseRequest.getId(),
                excuseRequest.getAttachmentOriginalName(),
                excuseRequest.getAttachmentContentType(),
                excuseRequest.getAttachmentSize(),
                excuseRequest.getUpdatedAt()
        );
    }
}
