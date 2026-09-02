package com.msa4lmsv2academic.domain.lecture.request;

import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "교수 담당 강의 조회 조건")
public record ProfessorLectureSearchRequestDTO(
        @Schema(description = "페이지 번호(1부터 시작)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "page는 1 이상이어야 합니다.")
        Integer page,

        @Schema(description = "페이지 크기. 100을 초과하면 100으로 제한", example = "20", defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        Integer size,

        @Schema(description = "학년도. 생략하면 전체 학년도를 조회합니다.", example = "2026")
        @Min(value = 1900, message = "academicYear는 1900 이상이어야 합니다.")
        @Max(value = 9999, message = "academicYear는 9999 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기. 생략하면 전체 학기를 조회합니다.", example = "FIRST",
                allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "강의 상태. 생략하면 전체 상태를 조회합니다.", example = "OPEN",
                allowableValues = {"OPEN", "CLOSED"})
        LectureStatus status,

        @Schema(description = "현재 학기 여부. 생략하면 전체 학기를 조회합니다.", example = "true")
        Boolean current
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
