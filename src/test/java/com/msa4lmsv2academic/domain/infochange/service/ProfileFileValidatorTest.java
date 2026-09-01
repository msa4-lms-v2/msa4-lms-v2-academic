package com.msa4lmsv2academic.domain.infochange.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.global.error.FileSizeExceededException;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ProfileFileValidatorTest {

    private final ProfileFileValidator validator = new ProfileFileValidator();

    @Test
    void acceptsJpegProfileImageAndPdfAttachments() {
        MockMultipartFile image = file(
                "profileImage", "profile.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );
        MockMultipartFile pdf = file(
                "attachments", "proof.pdf", "application/pdf", "%PDF-1.7".getBytes()
        );

        assertThatNoException().isThrownBy(() -> validator.validate(image, List.of(pdf)));
    }

    @Test
    void acceptsJpegAndPngAttachmentsForStudentRequests() {
        MockMultipartFile jpeg = file(
                "attachments", "residence.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );
        MockMultipartFile png = file(
                "attachments", "certificate.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        assertThatNoException().isThrownBy(() -> validator.validateStudent(null, List.of(jpeg, png)));
    }

    @Test
    void keepsProfessorAttachmentsPdfOnly() {
        MockMultipartFile jpeg = file(
                "attachments", "residence.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        assertThatThrownBy(() -> validator.validate(null, List.of(jpeg)))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFileWhoseMimeAndSignatureDoNotMatchPdfExtension() {
        MockMultipartFile disguised = file(
                "attachments", "proof.pdf", "application/pdf", "not-a-pdf".getBytes()
        );

        assertThatThrownBy(() -> validator.validateStudent(null, List.of(disguised)))
                .isInstanceOf(InvalidFileException.class);
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
}
