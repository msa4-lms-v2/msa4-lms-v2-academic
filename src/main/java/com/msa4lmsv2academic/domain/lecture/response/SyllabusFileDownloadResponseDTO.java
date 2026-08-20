package com.msa4lmsv2academic.domain.lecture.response;

public record SyllabusFileDownloadResponseDTO(
        String originalName,
        String contentType,
        byte[] content
) {
}
