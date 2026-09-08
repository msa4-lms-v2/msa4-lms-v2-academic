package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DepartmentTransferFileValidator {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> HWP_DECLARED_TYPES = Set.of(
            "application/x-hwp", "application/haansofthwp", "application/vnd.hancom.hwp",
            "application/octet-stream");
    private static final Set<String> HWPX_DECLARED_TYPES = Set.of(
            "application/hwp+zip", "application/x-hwp+zip", "application/vnd.hancom.hwpx",
            "application/zip", "application/octet-stream");
    private static final byte[] HWP_MARKER = "HWP Document File".getBytes(StandardCharsets.US_ASCII);

    public List<MultipartFile> validate(List<MultipartFile> files) {
        List<MultipartFile> present = files == null ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (present.size() != 2) {
            throw new InvalidFileException("학적 변경 첨부파일은 HWP 또는 HWPX 형식으로 정확히 2개 필요합니다.");
        }
        present.forEach(this::validateOne);
        return present;
    }

    private void validateOne(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new FileSizeExceededException("학적 변경 첨부파일은 파일당 10MB 이하여야 합니다.");
        }
        String extension = extension(file);
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String detected = detectedContentType(file);
        boolean hwp = "hwp".equals(extension) && HWP_DECLARED_TYPES.contains(declared)
                && "application/x-tika-msoffice".equals(detected) && contains(file, HWP_MARKER);
        boolean hwpx = "hwpx".equals(extension) && HWPX_DECLARED_TYPES.contains(declared)
                && "application/zip".equals(detected) && hasHwpxStructure(file);
        if (!hwp && !hwpx) {
            throw new InvalidFileException("학적 변경 첨부파일은 실제 HWP 또는 HWPX 형식만 허용됩니다.");
        }
    }

    private String detectedContentType(MultipartFile file) {
        try (TikaInputStream input = TikaInputStream.get(file.getInputStream())) {
            MediaType type = MimeTypes.getDefaultMimeTypes().detect(input, new Metadata(), new ParseContext());
            return type.toString().toLowerCase(Locale.ROOT);
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private boolean contains(MultipartFile file, byte[] marker) {
        try (InputStream input = new BufferedInputStream(file.getInputStream())) {
            int matched = 0;
            int current;
            while ((current = input.read()) != -1) {
                if ((byte) current == marker[matched]) {
                    if (++matched == marker.length) return true;
                } else {
                    matched = (byte) current == marker[0] ? 1 : 0;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private boolean hasHwpxStructure(MultipartFile file) {
        boolean mimetype = false;
        boolean content = false;
        boolean manifest = false;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 10_000) return false;
                String name = entry.getName().replace('\\', '/');
                if ("mimetype".equals(name)) mimetype = "application/hwp+zip".equals(readLimited(zip));
                else if ("Contents/content.hpf".equals(name)) content = true;
                else if ("META-INF/manifest.xml".equals(name)) manifest = true;
            }
            return mimetype && content && manifest;
        } catch (IOException exception) {
            throw new InvalidFileException("업로드 파일을 검사할 수 없습니다.", exception);
        }
    }

    private String readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[32];
        int remaining = 101;
        while (remaining > 0) {
            int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read < 0) break;
            output.write(buffer, 0, read);
            remaining -= read;
        }
        return output.size() > 100 ? "" : output.toString(StandardCharsets.US_ASCII).trim();
    }

    private String extension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
