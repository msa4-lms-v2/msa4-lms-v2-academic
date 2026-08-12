package com.msa4lmsv2academic.domain.professor.request;

import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 교수 목록 검색 조건")
public record ProfessorSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기(최대 100)", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "소속 학과 ID 정확 일치", example = "3")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "임용 연도 정확 일치. 1900년부터 현재 연도까지 허용", example = "2020", minimum = "1900")
        @Min(value = 1900, message = "hireYear는 1900 이상이어야 합니다.")
        Integer hireYear,

        @Schema(description = "Auth에서 동기화된 읽기 전용 계정 상태", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "교수 이름 또는 이메일의 대소문자 무시 부분 검색", example = "kim", maxLength = 100)
        @Size(max = 100, message = "keyword는 100자 이하여야 합니다.")
        String keyword
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
