package com.msa4lmsv2academic.domain.lecture.request;

import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "학생 수강신청 대상 강의 조회 조건")
public record AvailableLectureSearchRequestDTO(
        @Min(1) Integer page,
        @Min(1) Integer size,
        @Min(1900) @Max(9999) Short academicYear,
        SemesterTerm term,
        Long collegeId,
        Long departmentId,
        @Min(1) @Max(4) Byte targetGrade,
        String courseName,
        String professorName,
        LectureStatus status
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

    public LectureStatus resolvedStatus() {
        return status == null ? LectureStatus.OPEN : status;
    }
}
