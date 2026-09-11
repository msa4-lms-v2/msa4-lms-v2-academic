package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record LeaveRequestSearchRequestDTO(
        @Min(1) @Schema(description = "1부터 시작하는 페이지", defaultValue = "1", example = "1") Integer page,
        @Min(1) @Max(100) @Schema(description = "페이지 크기(1~100)", defaultValue = "20", example = "20") Integer size,
        @Schema(description = "신청 유형 필터", example = "GENERAL_LEAVE") LeaveRequestType requestType,
        @Schema(description = "처리 상태 필터", example = "PENDING") LeaveRequestStatus status,
        @Min(1) @Max(32767) @Schema(description = "적용 학년도 필터", example = "2027") Short targetYear,
        @Min(1) @Max(2) @Schema(description = "적용 학기 필터", example = "1") Byte targetSemester,
        @Positive @Schema(description = "학생 ID 필터. 학생은 본인 ID만 허용, 교수는 담당 학생만, 관리자는 전체 검색 가능", example = "1") Long studentId,
        @Size(max = 50) @Schema(description = "학생 이름 또는 학번 통합 검색어", maxLength = 50, example = "20241234") String keyword,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "신청일 시작(포함, YYYY-MM-DD)", format = "date", example = "2026-03-01") LocalDate requestedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(description = "신청일 종료(포함, YYYY-MM-DD)", format = "date", example = "2026-03-31") LocalDate requestedTo,
        @Schema(description = "생성 시각 및 ID 정렬 방향", defaultValue = "CREATED_AT_DESC", example = "CREATED_AT_DESC") LeaveRequestSort sort
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
    public LeaveRequestSort resolvedSort() { return sort == null ? LeaveRequestSort.CREATED_AT_DESC : sort; }

    @AssertTrue(message = "신청일 시작은 종료일보다 늦을 수 없습니다.")
    public boolean isRequestedPeriodValid() {
        return requestedFrom == null || requestedTo == null || !requestedFrom.isAfter(requestedTo);
    }
}
