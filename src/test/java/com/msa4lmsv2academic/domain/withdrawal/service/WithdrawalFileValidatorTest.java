package com.msa4lmsv2academic.domain.withdrawal.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class WithdrawalFileValidatorTest {

    private final WithdrawalFileValidator validator = new WithdrawalFileValidator();

    @Test
    void acceptsPdfHangulDocumentsAndSupportedRasterImages() throws IOException {
        List<MockMultipartFile> files = List.of(
                file("proof.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
                file("proof.hwp", "application/x-hwp", hwp()),
                file("proof.hwpx", "application/hwp+zip", hwpx()),
                file("proof.jpg", "image/jpeg", jpeg()),
                file("proof.png", "image/png", png()),
                file("proof.gif", "image/gif", gif()),
                file("proof.webp", "image/webp", webp())
        );

        files.forEach(file -> assertThatNoException()
                .as(file.getOriginalFilename())
                .isThrownBy(() -> validator.validateRequired(file)));
    }

    @Test
    void rejectsExcludedImageFormatsAndSvg() {
        List<MockMultipartFile> files = List.of(
                file("proof.bmp", "image/bmp", new byte[]{'B', 'M'}),
                file("proof.tiff", "image/tiff", new byte[]{'I', 'I', 0x2A, 0x00}),
                file("proof.heic", "image/heic", isoBaseMedia("heic")),
                file("proof.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8))
        );

        files.forEach(file -> assertThatThrownBy(() -> validator.validateRequired(file))
                .as(file.getOriginalFilename())
                .isInstanceOf(InvalidFileException.class));
    }

    @Test
    void rejectsFilesWhoseExtensionMimeAndSignatureDoNotAgree() {
        MockMultipartFile renamedPdf = file("proof.pdf", "application/pdf", "not-pdf".getBytes());
        MockMultipartFile renamedImage = file("proof.jpg", "image/jpeg", png());
        MockMultipartFile wrongMime = file("proof.png", "image/jpeg", png());

        assertThatThrownBy(() -> validator.validateRequired(renamedPdf)).isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validateRequired(renamedImage)).isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validateRequired(wrongMime)).isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsOversizedOrMissingEvidence() {
        byte[] oversized = new byte[(int) WithdrawalFileValidator.MAX_FILE_SIZE + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, oversized, 0, 5);

        assertThatThrownBy(() -> validator.validateRequired(file("large.pdf", "application/pdf", oversized)))
                .isInstanceOf(FileSizeExceededException.class);
        assertThatThrownBy(() -> validator.validateRequired(null)).isInstanceOf(InvalidFileException.class);
    }

    private MockMultipartFile file(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private byte[] jpeg() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    private byte[] gif() {
        return "GIF89a".getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] webp() {
        return new byte[]{'R', 'I', 'F', 'F', 0x04, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'};
    }

    private byte[] hwp() {
        byte[] oleSignature = {
                (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
        };
        byte[] marker = "HWP Document File".getBytes(StandardCharsets.US_ASCII);
        byte[] bytes = new byte[oleSignature.length + marker.length];
        System.arraycopy(oleSignature, 0, bytes, 0, oleSignature.length);
        System.arraycopy(marker, 0, bytes, oleSignature.length, marker.length);
        return bytes;
    }

    private byte[] hwpx() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeEntry(zipOutputStream, "mimetype", "application/hwp+zip");
            writeEntry(zipOutputStream, "Contents/content.hpf", "<opf:package/>");
            writeEntry(zipOutputStream, "META-INF/manifest.xml", "<manifest/>");
        }
        return outputStream.toByteArray();
    }

    private byte[] isoBaseMedia(String brand) {
        byte[] bytes = new byte[24];
        bytes[3] = 24;
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, bytes, 4, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, bytes, 16, 4);
        return bytes;
    }

    private void writeEntry(ZipOutputStream outputStream, String name, String content) throws IOException {
        outputStream.putNextEntry(new ZipEntry(name));
        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        outputStream.closeEntry();
    }
}
