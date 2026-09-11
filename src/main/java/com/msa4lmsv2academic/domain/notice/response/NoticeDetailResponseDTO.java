package com.msa4lmsv2academic.domain.notice.response;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeCategory;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공지사항 상세 응답")
public record NoticeDetailResponseDTO(
        @Schema(description = "공지사항 ID", example = "1")
        Long id,
        @Schema(description = "공지 제목", example = "2026학년도 2학기 수강신청 안내")
        String title,
        @Schema(description = "공지 본문", example = "수강신청 기간과 유의사항을 확인해 주세요.", nullable = true)
        String content,
        @Schema(description = "공지 분류", example = "IMPORTANT", allowableValues = {"NORMAL", "IMPORTANT"})
        NoticeCategory category,
        @Schema(description = "중요 공지를 일반 공지로 자동 전환할 날짜. 해당 날짜 00:00부터 일반 공지로 전환됩니다.", example = "2026-05-06", nullable = true)
        LocalDate normalTransitionDate,
        @Schema(description = "공지 대상 역할", example = "ALL", allowableValues = {"ALL", "STUDENT", "PROFESSOR"})
        NoticeTargetRole targetRole,
        @Schema(description = "작성자 이름", example = "관리자")
        String authorName,
        @Schema(description = "활성 여부", example = "true")
        boolean isActive,
        @Schema(description = "등록 일시", example = "2026-08-12T15:30:00", format = "date-time")
        LocalDateTime createdAt,
        @Schema(description = "첨부파일 목록") List<NoticeAttachmentResponseDTO> attachments
) {

    public static NoticeDetailResponseDTO from(Notice notice) {
        return from(notice, List.of());
    }

    public static NoticeDetailResponseDTO from(Notice notice, List<NoticeAttachmentResponseDTO> attachments) {
        return new NoticeDetailResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCategory(),
                notice.getNormalTransitionDate(),
                notice.getTargetRole(),
                notice.getAuthor().getName(),
                notice.isActive(),
                notice.getCreatedAt(),
                attachments
        );
    }
}
