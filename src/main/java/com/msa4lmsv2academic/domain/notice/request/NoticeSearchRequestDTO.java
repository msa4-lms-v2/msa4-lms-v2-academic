package com.msa4lmsv2academic.domain.notice.request;

import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "공지사항 목록 검색 조건")
public record NoticeSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "제목·본문 검색어", example = "수강신청", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword,

        @Schema(
                description = "공지 대상 역할. STUDENT와 PROFESSOR는 ALL 또는 본인 역할만 지정할 수 있습니다.",
                example = "STUDENT",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"}
        )
        NoticeTargetRole targetRole,

        @Schema(
                description = "활성 상태. ADMIN이 생략하면 전체 상태를 조회하며 일반 사용자는 항상 활성 공지만 조회합니다.",
                example = "true"
        )
        Boolean active
) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public int resolvedPage() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? DEFAULT_SIZE : size, MAX_SIZE);
    }
}
