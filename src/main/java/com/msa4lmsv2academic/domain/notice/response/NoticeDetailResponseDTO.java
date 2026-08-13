package com.msa4lmsv2academic.domain.notice.response;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "공지사항 상세 응답")
public record NoticeDetailResponseDTO(
        @Schema(description = "공지사항 ID", example = "1")
        Long id,
        @Schema(description = "공지 제목", example = "2026학년도 2학기 수강신청 안내")
        String title,
        @Schema(description = "공지 본문", example = "수강신청 기간과 유의사항을 확인해 주세요.", nullable = true)
        String content,
        @Schema(description = "공지 대상 역할", example = "ALL", allowableValues = {"ALL", "STUDENT", "PROFESSOR"})
        NoticeTargetRole targetRole,
        @Schema(description = "활성 여부", example = "true")
        boolean isActive,
        @Schema(description = "등록 일시", example = "2026-08-12T15:30:00", format = "date-time")
        LocalDateTime createdAt
) {

    public static NoticeDetailResponseDTO from(Notice notice) {
        return new NoticeDetailResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getTargetRole(),
                notice.isActive(),
                notice.getCreatedAt()
        );
    }
}
