package com.msa4lmsv2academic.domain.infochange.request;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequesterType;
import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "관리자용 학생·교수 통합 프로필 변경 신청 검색 조건")
public record AdminInfoChangeRequestSearchRequestDTO(
        @Schema(description = "신청자 이름 부분 검색", example = "김", nullable = true)
        @Size(max = 50) String keyword,

        @Schema(description = "신청자 유형", example = "STUDENT", nullable = true)
        InfoChangeRequesterType requesterType,

        @Schema(description = "신청 상태", example = "REQUESTED", nullable = true)
        InfoChangeRequestStatus status,

        @Schema(description = "신청일 시작(포함)", example = "2026-09-01", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedFrom,

        @Schema(description = "신청일 종료(포함)", example = "2026-09-30", nullable = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedTo,

        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(1) Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(1) Integer size
) {
    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }
}
