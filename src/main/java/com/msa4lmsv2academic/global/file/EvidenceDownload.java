package com.msa4lmsv2academic.global.file;

public record EvidenceDownload(String originalName, byte[] content, String contentType) {
    public EvidenceDownload(String originalName, byte[] content) {
        this(originalName, content, "application/pdf");
    }
}
