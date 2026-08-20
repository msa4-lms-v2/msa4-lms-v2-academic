package com.msa4lmsv2academic.domain.lecture.response;

public record SyllabusFileDownloadTarget(
        String originalName,
        String storedName,
        String contentType
) {
}
