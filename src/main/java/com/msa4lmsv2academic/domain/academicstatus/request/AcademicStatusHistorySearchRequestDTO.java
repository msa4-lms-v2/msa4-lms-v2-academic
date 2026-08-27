package com.msa4lmsv2academic.domain.academicstatus.request;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistorySourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "학적 변경 이력 검색 조건. 모든 필터는 호출자의 조회 범위 안에서 AND로 적용")
public record AcademicStatusHistorySearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1", minimum = "1")
        @Min(1) Integer page,

        @Schema(description = "페이지 크기. 100 초과는 100으로 제한", example = "20", defaultValue = "20", minimum = "1")
        @Min(1) Integer size,

        @Schema(description = "현재 학생 이름 부분 검색. 앞뒤 공백 제거, 빈 값은 검색하지 않음. 이메일·사유 검색 제외",
                example = "김학생", maxLength = 100)
        @Size(max = 100) String keyword,

        @Schema(description = "Academic 학생 ID 정확 일치. 권한 밖 ID는 빈 목록 반환", example = "1", minimum = "1")
        @Positive Long studentId,

        @Schema(description = "현재 소속 학과 ID 정확 일치", example = "130", minimum = "1")
        @Positive Long departmentId,

        @Schema(description = "변경 전 학적 상태 정확 일치", example = "ENROLLED")
        AcademicStatus previousStatus,

        @Schema(description = "변경 후 학적 상태 정확 일치", example = "ON_LEAVE")
        AcademicStatus newStatus,

        @Schema(description = "상태 전이 원인 정확 일치. 휴학과 복학은 모두 LEAVE_REQUEST", example = "LEAVE_REQUEST")
        AcademicStatusHistorySourceType sourceType,

        @Schema(description = "이력 기록일 시작(포함, KST). YYYY-MM-DD, 1000-01-01 이상", example = "2026-08-01", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

        @Schema(description = "이력 기록일 종료(해당 날짜 전체 포함, KST). YYYY-MM-DD, 9999-12-31 이하. fromDate 이상",
                example = "2026-08-31", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

        @Schema(description = "createdAt 정렬 방향. 같은 시각이면 historyId도 같은 방향으로 정렬",
                example = "desc", defaultValue = "desc", allowableValues = {"asc", "desc"})
        @Pattern(regexp = "asc|desc") String sortDirection
) {
    private static final LocalDate MIN_DATE = LocalDate.of(1000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    public int resolvedPage() {
        return page == null ? 1 : page;
    }

    public int resolvedSize() {
        return Math.min(size == null ? 20 : size, 100);
    }

    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    @Schema(hidden = true)
    @AssertTrue(message = "날짜는 1000-01-01~9999-12-31 범위이며 fromDate는 toDate 이하여야 합니다.")
    public boolean isValidDateRange() {
        return (fromDate == null || !fromDate.isBefore(MIN_DATE) && !fromDate.isAfter(MAX_DATE))
                && (toDate == null || !toDate.isBefore(MIN_DATE) && !toDate.isAfter(MAX_DATE))
                && (fromDate == null || toDate == null || !fromDate.isAfter(toDate));
    }
}
