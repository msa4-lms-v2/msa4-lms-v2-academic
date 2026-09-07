package com.msa4lmsv2academic.domain.transfer.service;

public record StoredTransferDocument(
        String originalName,
        String storedName,
        String contentType,
        long size
) { }
