package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProfileFileValidator {

    private static final long PROFILE_IMAGE_MAX_SIZE = 5L * 1024 * 1024;
    private static final long ATTACHMENT_MAX_SIZE = 10L * 1024 * 1024;
    private static final long REQUEST_MAX_SIZE = 20L * 1024 * 1024;
    private static final int ATTACHMENT_MAX_COUNT = 5;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    public void validate(MultipartFile profileImage, List<MultipartFile> attachments) {
        List<MultipartFile> presentAttachments = attachments == null
                ? List.of()
                : attachments.stream().filter(this::hasContent).toList();
        if (presentAttachments.size() > ATTACHMENT_MAX_COUNT) {
            throw new InvalidFileException("증빙 PDF는 최대 5개까지 첨부할 수 있습니다.");
        }

        long totalSize = 0;
        if (hasContent(profileImage)) {
            validateProfileImage(profileImage);
            totalSize += profileImage.getSize();
        }
        for (MultipartFile attachment : presentAttachments) {
            validatePdf(attachment);
            totalSize += attachment.getSize();
        }
        if (totalSize > REQUEST_MAX_SIZE) {
            throw new FileSizeExceededException("프로필 변경 신청의 전체 파일 크기는 20MB 이하여야 합니다.");
        }
    }

    private void validateProfileImage(MultipartFile file) {
        if (file.getSize() > PROFILE_IMAGE_MAX_SIZE) {
            throw new FileSizeExceededException("프로필 이미지는 5MB 이하여야 합니다.");
        }
        String extension = extension(file);
        String contentType = normalizedContentType(file);
        boolean jpeg = ("jpg".equals(extension) || "jpeg".equals(extension))
                && "image/jpeg".equals(contentType)
                && hasJpegSignature(file);
        boolean png = "png".equals(extension)
                && "image/png".equals(contentType)
                && hasSignature(file, PNG_SIGNATURE);
        if (!jpeg && !png) {
            throw new InvalidFileException("프로필 이미지는 JPEG 또는 PNG 형식만 허용됩니다.");
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file.getSize() > ATTACHMENT_MAX_SIZE) {
            throw new FileSizeExceededException("증빙 PDF는 파일당 10MB 이하여야 합니다.");
        }
        if (!"pdf".equals(extension(file))
                || !"application/pdf".equals(normalizedContentType(file))
                || !hasSignature(file, PDF_SIGNATURE)) {
            throw new InvalidFileException("증빙 파일은 PDF 형식만 허용됩니다.");
        }
    }

    private boolean hasJpegSignature(MultipartFile file) {
        byte[] signature = readPrefix(file, 3);
        return signature.length >= 3
                && signature[0] == (byte) 0xFF
                && signature[1] == (byte) 0xD8
                && signature[2] == (byte) 0xFF;
    }

    private boolean hasSignature(MultipartFile file, byte[] expected) {
        byte[] signature = readPrefix(file, expected.length);
        if (signature.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (signature[index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private byte[] readPrefix(MultipartFile file, int length) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(length);
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private String extension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return "";
        }
        int delimiter = filename.lastIndexOf('.');
        return delimiter < 0 ? "" : filename.substring(delimiter + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private boolean hasContent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
}
