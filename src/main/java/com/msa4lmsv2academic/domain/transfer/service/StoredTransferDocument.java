package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;

public record StoredTransferDocument(
        TransferDocumentType type,
        String originalName,
        String storedName,
        String contentType,
        long size
) { }
