package com.msa4lmsv2academic.domain.infochange.service;

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

class ProfileFileValidatorTest {

    private final ProfileFileValidator validator = new ProfileFileValidator();

    @Test
    void acceptsRasterProfileImageAndSupportedAttachmentsForBothRoles() throws IOException {
        MockMultipartFile image = file(
                "profileImage", "profile.webp", "image/webp", webp()
        );
        MockMultipartFile pdf = file(
                "attachments", "proof.pdf", "application/pdf", "%PDF-1.7".getBytes()
        );
        MockMultipartFile hwp = file(
                "attachments", "proof.hwp", "application/x-hwp", hwp()
        );
        MockMultipartFile hwpx = file(
                "attachments", "proof.hwpx", "application/hwp+zip", hwpx()
        );

        assertThatNoException().isThrownBy(() -> validator.validate(image, List.of(pdf, hwp, hwpx)));
    }

    @Test
    void acceptsEveryConfiguredRasterImageType() {
        List<MockMultipartFile> files = List.of(
                file("profileImage", "profile.jpg", "image/jpeg", jpeg()),
                file("profileImage", "profile.png", "image/png", png()),
                file("profileImage", "profile.gif", "image/gif", gif()),
                file("profileImage", "profile.webp", "image/webp", webp())
        );

        files.forEach(image ->
                assertThatNoException()
                        .as(image.getOriginalFilename())
                        .isThrownBy(() -> validator.validate(image, List.of()))
        );
    }

    @Test
    void rejectsFileWhoseMimeAndSignatureDoNotMatchPdfExtension() {
        MockMultipartFile disguised = file(
                "attachments", "proof.pdf", "application/pdf", "not-a-pdf".getBytes()
        );

        assertThatThrownBy(() -> validator.validate(null, List.of(disguised)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsImageWhoseExtensionMimeAndDetectedTypeDoNotAgree() {
        MockMultipartFile disguised = file(
                "profileImage", "profile.jpg", "image/jpeg", png()
        );

        assertThatThrownBy(() -> validator.validate(disguised, List.of()))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsGenericOfficeAndZipContainersRenamedAsHangulDocuments() throws IOException {
        MockMultipartFile disguisedHwp = file(
                "attachments",
                "proof.hwp",
                "application/x-hwp",
                new byte[]{
                        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                        (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
                }
        );
        MockMultipartFile disguisedHwpx = file(
                "attachments", "proof.hwpx", "application/hwp+zip", genericZip()
        );

        assertThatThrownBy(() -> validator.validate(null, List.of(disguisedHwp)))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validate(null, List.of(disguisedHwpx)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsSvgForProfileAndAttachment() {
        MockMultipartFile svg = file(
                "profileImage",
                "profile.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> validator.validate(svg, List.of()))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validate(null, List.of(svg)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsNonWebImageFormats() {
        List<MockMultipartFile> files = List.of(
                file("profileImage", "profile.bmp", "image/bmp", new byte[]{'B', 'M'}),
                file("profileImage", "profile.tiff", "image/tiff", new byte[]{'I', 'I', 0x2A, 0x00}),
                file("profileImage", "profile.heic", "image/heic", isoBaseMedia("heic"))
        );

        files.forEach(image -> assertThatThrownBy(() -> validator.validate(image, List.of()))
                .isInstanceOf(InvalidFileException.class));
    }

    @Test
    void rejectsMoreThanFiveAttachments() {
        MockMultipartFile pdf = file(
                "attachments", "proof.pdf", "application/pdf", "%PDF-1.7".getBytes()
        );

        assertThatThrownBy(() -> validator.validate(null, List.of(pdf, pdf, pdf, pdf, pdf, pdf)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsProfileImageLargerThanFiveMegabytes() {
        byte[] oversizedJpeg = new byte[5 * 1024 * 1024 + 1];
        oversizedJpeg[0] = (byte) 0xFF;
        oversizedJpeg[1] = (byte) 0xD8;
        oversizedJpeg[2] = (byte) 0xFF;
        MockMultipartFile image = file("profileImage", "profile.jpg", "image/jpeg", oversizedJpeg);

        assertThatThrownBy(() -> validator.validate(image, List.of()))
                .isInstanceOf(FileSizeExceededException.class);
    }

    private MockMultipartFile file(String name, String filename, String contentType, byte[] content) {
        return new MockMultipartFile(name, filename, contentType, content);
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

    private byte[] isoBaseMedia(String brand) {
        byte[] bytes = new byte[24];
        bytes[3] = 24;
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, bytes, 4, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, bytes, 16, 4);
        return bytes;
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

    private byte[] genericZip() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeEntry(zipOutputStream, "document.txt", "not an HWPX document");
        }
        return outputStream.toByteArray();
    }

    private void writeEntry(ZipOutputStream outputStream, String name, String content) throws IOException {
        outputStream.putNextEntry(new ZipEntry(name));
        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        outputStream.closeEntry();
    }
}
