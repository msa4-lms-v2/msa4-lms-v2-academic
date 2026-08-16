package com.msa4lmsv2academic.domain.infochange.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Schema(description = "학적 정보 변경 신청(multipart/form-data)")
public record StudentInfoChangeRequestCreateDTO(
        @Size(max = 50)
        String newName,

        @Size(max = 20)
        String newPhoneNumber,

        @Email
        @Size(max = 100)
        String newEmail,

        @Size(max = 255)
        String newAddress,

        @Schema(description = "변경할 프로필 사진")
        MultipartFile profileImage,

        @Schema(description = "증빙 파일(여러 개 가능)")
        List<MultipartFile> attachments,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}
