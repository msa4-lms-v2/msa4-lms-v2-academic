package com.msa4lmsv2academic.global.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EvidenceFileValidator {

    public static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    public void validateOptional(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        validate(file);
    }

    public void validateRequired(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("PDF 증빙 파일은 필수입니다.");
        }
        validate(file);
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException("증빙 파일은 10MB 이하여야 합니다.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidFileException("PDF 확장자 파일만 업로드할 수 있습니다.");
        }
        if (originalName.length() > 255) {
            throw new InvalidFileException("파일명은 255자 이하여야 합니다.");
        }
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new InvalidFileException("파일 MIME 타입은 application/pdf여야 합니다.");
        }
        validateSignature(file);
    }

    private void validateSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);
            if (signature.length != PDF_SIGNATURE.length) {
                throw new InvalidFileException("유효한 PDF 파일이 아닙니다.");
            }
            for (int index = 0; index < PDF_SIGNATURE.length; index++) {
                if (signature[index] != PDF_SIGNATURE[index]) {
                    throw new InvalidFileException("유효한 PDF 파일이 아닙니다.");
                }
            }
        } catch (IOException exception) {
            throw new InvalidFileException("PDF 파일을 읽을 수 없습니다.", exception);
        }
    }
}
