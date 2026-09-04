package com.msa4lmsv2academic.domain.infochange.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "교수 프로필 변경 신청(multipart/form-data)")
public record ProfessorInfoChangeRequestCreateDTO(
        @Schema(description = "변경할 이름", example = "김교수", nullable = true)
        @Size(max = 50)
        String newName,

        @Schema(description = "변경할 전화번호", example = "010-1234-5678", nullable = true)
        @Size(max = 20)
        String newPhoneNumber,

        @Schema(description = "변경할 이메일", example = "professor@example.com", nullable = true)
        @Email
        @Size(max = 100)
        String newEmail,

        @Schema(description = "변경할 주소", example = "서울특별시 중구", nullable = true)
        @Size(max = 255)
        String newAddress,

        @Schema(description = "변경할 프로필 사진(JPEG/PNG/GIF/WebP, 최대 5MB)", nullable = true)
        MultipartFile profileImage,

        @Schema(description = "증빙 PDF, 래스터 이미지 또는 HWP/HWPX(파일당 최대 10MB, 최대 5개)", nullable = true)
        List<MultipartFile> attachments,

        @Schema(description = "변경 신청 사유", example = "연락처 변경", maxLength = 500)
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
