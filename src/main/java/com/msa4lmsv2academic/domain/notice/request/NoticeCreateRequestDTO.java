package com.msa4lmsv2academic.domain.notice.request;

import com.msa4lmsv2academic.domain.notice.entity.NoticeCategory;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

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
                maxLength = 5000,
                nullable = true
        )
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        String content,

        @Schema(description = "공지 분류. 생략하면 일반 공지로 생성됩니다.", example = "IMPORTANT",
                allowableValues = {"NORMAL", "IMPORTANT"})
        NoticeCategory category,

        @Schema(description = "중요 공지를 일반 공지로 자동 전환할 날짜. 해당 날짜 00:00부터 일반 공지로 전환됩니다. IMPORTANT일 때 필수입니다.",
                example = "2026-05-06", format = "date", nullable = true)
        LocalDate normalTransitionDate,

        @Schema(
                description = "공지 대상 역할",
                example = "ALL",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "targetRole은 필수입니다.")
        NoticeTargetRole targetRole
) {

    public NoticeCreateRequestDTO(String title, String content, NoticeTargetRole targetRole) {
        this(title, content, null, null, targetRole);
    }
}
