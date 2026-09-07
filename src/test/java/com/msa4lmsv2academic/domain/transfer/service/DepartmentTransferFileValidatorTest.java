package com.msa4lmsv2academic.domain.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
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

class DepartmentTransferFileValidatorTest {
    private final DepartmentTransferFileValidator validator = new DepartmentTransferFileValidator();

    @Test
    void acceptsRealHwpAndStructuredHwpx() throws IOException {
        var files = validator.validate(List.of(realHwp(), hwpx()));
        assertThat(files).hasSize(2);
    }

    @Test
    void rejectsWrongCountAndSpoofedExtension() {
        assertThatThrownBy(() -> validator.validate(List.of(realHwp())))
                .isInstanceOf(InvalidFileException.class);
        var fake = new MockMultipartFile("files", "가짜.hwp", "application/x-hwp",
                "not-hwp".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> validator.validate(List.of(fake, realHwp())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFileOverTenMegabytes() {
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("files", "큰파일.hwp", "application/x-hwp", oversized);
        assertThatThrownBy(() -> validator.validate(List.of(file, realHwp())))
                .isInstanceOf(FileSizeExceededException.class);
    }

    private MockMultipartFile realHwp() {
        try (var input = getClass().getClassLoader()
                .getResourceAsStream("templates/department-transfer/self-introduction.hwp")) {
            if (input == null) throw new IllegalStateException("테스트 HWP 양식을 찾을 수 없습니다.");
            return new MockMultipartFile("files", "자기소개서.hwp", "application/x-hwp", input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MockMultipartFile hwpx() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            add(zip, "mimetype", "application/hwp+zip");
            add(zip, "Contents/content.hpf", "<package/>");
            add(zip, "META-INF/manifest.xml", "<manifest/>");
        }
        return new MockMultipartFile("files", "학업계획서.hwpx", "application/hwp+zip", output.toByteArray());
    }

    private void add(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.US_ASCII));
        zip.closeEntry();
    }
}
