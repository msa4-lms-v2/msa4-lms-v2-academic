package com.msa4lmsv2academic.domain.notice.response;

import com.msa4lmsv2academic.domain.notice.entity.NoticeAttachment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 첨부파일 정보")
public record NoticeAttachmentResponseDTO(
        @Schema(description = "첨부파일 ID", example = "1") Long id,
        @Schema(description = "원본 파일명", example = "수강신청_안내.pdf") String fileName,
        @Schema(description = "MIME 타입", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(byte)", example = "102400") long fileSize,
        @Schema(description = "1일간 유효한 임시 다운로드 URL") String downloadUrl
) {

    public static NoticeAttachmentResponseDTO from(NoticeAttachment attachment, String downloadUrl) {
        return new NoticeAttachmentResponseDTO(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                downloadUrl
        );
    }
}
