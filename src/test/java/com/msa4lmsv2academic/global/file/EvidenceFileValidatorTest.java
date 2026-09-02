package com.msa4lmsv2academic.global.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class EvidenceFileValidatorTest {

    private final EvidenceFileValidator validator = new EvidenceFileValidator();

    @Test
    void acceptsPdfExtensionMimeAndSignature() {
        MockMultipartFile file = pdf("evidence.pdf", "%PDF-1.7\ncontent".getBytes());

        assertThatCode(() -> validator.validateRequired(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRenamedOrWrongMimeFile() {
        MockMultipartFile renamed = new MockMultipartFile(
                "attachment", "evidence.pdf", "application/pdf", "not-pdf".getBytes()
        );
        MockMultipartFile wrongMime = new MockMultipartFile(
                "attachment", "evidence.pdf", "image/png", "%PDF-1.7".getBytes()
        );

        assertThatThrownBy(() -> validator.validateRequired(renamed))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validateRequired(wrongMime))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] oversized = new byte[(int) EvidenceFileValidator.MAX_FILE_SIZE + 1];
        System.arraycopy("%PDF-".getBytes(), 0, oversized, 0, 5);

        assertThatThrownBy(() -> validator.validateRequired(pdf("large.pdf", oversized)))
                .isInstanceOf(FileSizeExceededException.class);
    }

    @Test
    void optionalEvidenceAllowsMissingFile() {
        assertThatCode(() -> validator.validateOptional(null)).doesNotThrowAnyException();
    }

    private MockMultipartFile pdf(String name, byte[] content) {
        return new MockMultipartFile("attachment", name, "application/pdf", content);
    }
}
