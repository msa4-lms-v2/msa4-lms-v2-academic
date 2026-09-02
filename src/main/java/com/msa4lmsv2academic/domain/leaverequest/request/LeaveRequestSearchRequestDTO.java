package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record LeaveRequestSearchRequestDTO(
        @Min(1) @Schema(description = "1부터 시작하는 페이지", defaultValue = "1", example = "1") Integer page,
        @Min(1) @Max(100) @Schema(description = "페이지 크기(1~100)", defaultValue = "20", example = "20") Integer size,
        @Schema(description = "신청 유형 필터", example = "GENERAL_LEAVE") LeaveRequestType requestType,
        @Schema(description = "처리 상태 필터", example = "PENDING") LeaveRequestStatus status,
        @Min(1) @Max(32767) @Schema(description = "적용 학년도 필터", example = "2027") Short targetYear,
        @Min(1) @Max(2) @Schema(description = "적용 학기 필터", example = "1") Byte targetSemester,
        @Positive @Schema(description = "학생 ID 필터. 학생은 본인 ID만 허용, 관리자는 전체 검색 가능", example = "1") Long studentId,
        @Schema(description = "생성 시각 및 ID 정렬 방향", defaultValue = "CREATED_AT_DESC", example = "CREATED_AT_DESC") LeaveRequestSort sort
) {
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : size; }
    public LeaveRequestSort resolvedSort() { return sort == null ? LeaveRequestSort.CREATED_AT_DESC : sort; }
}
