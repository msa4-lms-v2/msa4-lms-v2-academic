package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProfileFileValidator {

    private static final long PROFILE_IMAGE_MAX_SIZE = 5L * 1024 * 1024;
    private static final long ATTACHMENT_MAX_SIZE = 10L * 1024 * 1024;
    private static final long REQUEST_MAX_SIZE = 20L * 1024 * 1024;
    private static final int ATTACHMENT_MAX_COUNT = 5;

    private static final Map<String, Set<String>> IMAGE_CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp"))
    );
    private static final Set<String> HWP_DECLARED_CONTENT_TYPES = Set.of(
            "application/x-hwp",
            "application/haansofthwp",
            "application/vnd.hancom.hwp",
            "application/octet-stream"
    );
    private static final Set<String> HWPX_DECLARED_CONTENT_TYPES = Set.of(
            "application/hwp+zip",
            "application/x-hwp+zip",
            "application/vnd.hancom.hwpx",
            "application/zip",
            "application/octet-stream"
    );
    private static final byte[] HWP_DOCUMENT_SIGNATURE = "HWP Document File".getBytes(StandardCharsets.US_ASCII);
    private static final String HWPX_MIMETYPE = "application/hwp+zip";
    private static final int HWPX_MAX_ENTRY_COUNT = 10_000;
    private static final int HWPX_MIMETYPE_MAX_SIZE = 100;

    public void validate(MultipartFile profileImage, List<MultipartFile> attachments) {
        validateFiles(profileImage, attachments);
    }

    private void validateFiles(MultipartFile profileImage, List<MultipartFile> attachments) {
        List<MultipartFile> presentAttachments = attachments == null
                ? List.of()
                : attachments.stream().filter(this::hasContent).toList();
        if (presentAttachments.size() > ATTACHMENT_MAX_COUNT) {
            throw new InvalidFileException("증빙 파일은 최대 5개까지 첨부할 수 있습니다.");
        }

        long totalSize = 0;
        if (hasContent(profileImage)) {
            validateProfileImage(profileImage);
            totalSize += profileImage.getSize();
        }
        for (MultipartFile attachment : presentAttachments) {
            validateAttachment(attachment);
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
        if (!isRasterImage(file)) {
            throw new InvalidFileException(
                    "프로필 이미지는 JPEG, PNG, GIF 또는 WebP 형식만 허용됩니다."
            );
        }
    }

    private void validateAttachment(MultipartFile file) {
        if (file.getSize() > ATTACHMENT_MAX_SIZE) {
            throw new FileSizeExceededException("증빙 파일은 파일당 10MB 이하여야 합니다.");
        }
        if (isPdf(file) || isRasterImage(file) || isHwp(file) || isHwpx(file)) {
            return;
        }
        throw new InvalidFileException(
                "증빙 파일은 PDF, 이미지(JPEG, PNG, GIF, WebP), HWP 또는 HWPX 형식만 허용됩니다."
        );
    }

    private boolean isPdf(MultipartFile file) {
        return "pdf".equals(extension(file))
                && "application/pdf".equals(normalizedContentType(file))
                && "application/pdf".equals(detectedContentType(file));
    }

    private boolean isRasterImage(MultipartFile file) {
        String extension = extension(file);
        Set<String> expectedContentTypes = IMAGE_CONTENT_TYPES_BY_EXTENSION.get(extension);
        return expectedContentTypes != null
                && expectedContentTypes.contains(normalizedContentType(file))
                && expectedContentTypes.contains(detectedContentType(file));
    }

    private boolean isHwp(MultipartFile file) {
        return "hwp".equals(extension(file))
                && HWP_DECLARED_CONTENT_TYPES.contains(normalizedContentType(file))
                && "application/x-tika-msoffice".equals(detectedContentType(file))
                && containsBytes(file, HWP_DOCUMENT_SIGNATURE);
    }

    private boolean isHwpx(MultipartFile file) {
        return "hwpx".equals(extension(file))
                && HWPX_DECLARED_CONTENT_TYPES.contains(normalizedContentType(file))
                && "application/zip".equals(detectedContentType(file))
                && hasHwpxStructure(file);
    }

    private String detectedContentType(MultipartFile file) {
        try (TikaInputStream inputStream = TikaInputStream.get(file.getInputStream())) {
            MediaType mediaType = MimeTypes.getDefaultMimeTypes().detect(
                    inputStream,
                    new Metadata(),
                    new ParseContext()
            );
            return mediaType.toString().toLowerCase(Locale.ROOT);
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private boolean containsBytes(MultipartFile file, byte[] expected) {
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            int matched = 0;
            int current;
            while ((current = inputStream.read()) != -1) {
                if ((byte) current == expected[matched]) {
                    matched++;
                    if (matched == expected.length) {
                        return true;
                    }
                } else {
                    matched = (byte) current == expected[0] ? 1 : 0;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private boolean hasHwpxStructure(MultipartFile file) {
        boolean hasContentManifest = false;
        boolean hasPackageManifest = false;
        boolean hasValidMimetype = false;
        int entryCount = 0;
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > HWPX_MAX_ENTRY_COUNT) {
                    return false;
                }
                String entryName = entry.getName().replace('\\', '/');
                if ("mimetype".equals(entryName)) {
                    hasValidMimetype = HWPX_MIMETYPE.equals(readLimitedText(zipInputStream));
                } else if ("Contents/content.hpf".equals(entryName)) {
                    hasContentManifest = true;
                } else if ("META-INF/manifest.xml".equals(entryName)) {
                    hasPackageManifest = true;
                }
            }
            return hasValidMimetype && hasContentManifest && hasPackageManifest;
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private String readLimitedText(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[32];
        int remaining = HWPX_MIMETYPE_MAX_SIZE + 1;
        while (remaining > 0) {
            int read = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read < 0) {
                break;
            }
            outputStream.write(buffer, 0, read);
            remaining -= read;
        }
        if (outputStream.size() > HWPX_MIMETYPE_MAX_SIZE) {
            return "";
        }
        return outputStream.toString(StandardCharsets.US_ASCII).trim();
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
