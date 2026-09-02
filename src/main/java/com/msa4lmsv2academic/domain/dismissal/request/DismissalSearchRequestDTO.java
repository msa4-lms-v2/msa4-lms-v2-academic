package com.msa4lmsv2academic.domain.dismissal.request;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalReasonType;
import com.msa4lmsv2academic.domain.dismissal.entity.DismissalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Sort;

public record DismissalSearchRequestDTO(
        @Min(1) @Schema(description = "페이지(1부터)", defaultValue = "1", example = "1") Integer page,
        @Min(1) @Schema(description = "페이지 크기. 100 초과는 100으로 제한", defaultValue = "20", example = "20") Integer size,
        @Positive @Schema(description = "학생 ID 필터", example = "1") Long studentId,
        @Positive @Schema(description = "학생의 현재 학과 ID 필터", example = "130") Long departmentId,
        @Size(max = 50) @Schema(description = "현재 학생 이름 부분 검색(최대 50자)", example = "김학생") String studentName,
        @Schema(description = "제적 사유 종류 필터", example = "DISCIPLINARY") DismissalReasonType reasonType,
        @Schema(description = "후보 상태 필터. 생략하면 전체", example = "PENDING") DismissalStatus status,
        @Schema(description = "등록 시각 및 ID 정렬 방향", defaultValue = "DESC", example = "DESC") Sort.Direction direction
) {
    public DismissalSearchRequestDTO { studentName = studentName == null ? null : studentName.strip(); }
    public int resolvedPage() { return page == null ? 1 : page; }
    public int resolvedSize() { return size == null ? 20 : Math.min(size, 100); }
    public boolean ascending() { return direction == Sort.Direction.ASC; }
}
