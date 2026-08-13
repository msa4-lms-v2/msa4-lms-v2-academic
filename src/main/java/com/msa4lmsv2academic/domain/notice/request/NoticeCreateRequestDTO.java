package com.msa4lmsv2academic.domain.notice.request;

import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "공지사항 등록 요청")
public record NoticeCreateRequestDTO(
        @Schema(
                description = "공지 제목. 앞뒤 공백은 제거됩니다.",
                example = "2026학년도 2학기 수강신청 안내",
                minLength = 1,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @Schema(
                description = "공지 본문. 생략하거나 공백이면 null로 저장됩니다.",
                example = "수강신청 기간과 유의사항을 확인해 주세요.",
                nullable = true
        )
        String content,

        @Schema(
                description = "공지 대상 역할",
                example = "ALL",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "targetRole은 필수입니다.")
        NoticeTargetRole targetRole
) {
}
