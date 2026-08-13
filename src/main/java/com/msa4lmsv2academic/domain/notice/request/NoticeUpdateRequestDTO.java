package com.msa4lmsv2academic.domain.notice.request;

import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "공지사항 부분 수정 요청")
public record NoticeUpdateRequestDTO(
        @Schema(description = "변경할 제목. 생략하거나 null이면 기존 값 유지", example = "수강신청 일정 변경 안내",
                minLength = 1, maxLength = 100)
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "변경할 본문. 생략하거나 null이면 기존 값 유지, 공백 문자열이면 본문 제거",
                example = "변경된 일정을 확인해 주세요.", nullable = true)
        String content,

        @Schema(description = "변경할 대상 역할. 생략하거나 null이면 기존 값 유지", example = "STUDENT",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"})
        NoticeTargetRole targetRole,

        @Schema(description = "변경할 활성 상태. 생략하거나 null이면 기존 값 유지", example = "true")
        Boolean isActive
) {

    public boolean hasAnyUpdateField() {
        return title != null || content != null || targetRole != null || isActive != null;
    }
}
