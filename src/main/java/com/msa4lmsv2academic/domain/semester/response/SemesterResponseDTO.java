package com.msa4lmsv2academic.domain.semester.response;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "학기 응답")
public record SemesterResponseDTO(
        @Schema(description = "학기 ID", example = "1")
        Long id,
        @Schema(description = "학년도", example = "2026")
        short academicYear,
        @Schema(description = "학기 구분", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,
        @Schema(description = "개강일", example = "2026-03-02", format = "date")
        LocalDate startDate,
        @Schema(description = "종강일", example = "2026-06-19", format = "date")
        LocalDate endDate,
        @Schema(description = "수강신청 시작 일시", example = "2026-02-16T09:00:00", format = "date-time")
        LocalDateTime enrollmentStartAt,
        @Schema(description = "수강신청 종료 일시", example = "2026-02-20T18:00:00", format = "date-time")
        LocalDateTime enrollmentEndAt,
        @Schema(description = "현재 학기 여부", example = "true")
        boolean isCurrent
) {

    public static SemesterResponseDTO from(Semester semester) {
        return new SemesterResponseDTO(
                semester.getId(),
                semester.getAcademicYear(),
                semester.getTerm(),
                semester.getStartDate(),
                semester.getEndDate(),
                semester.getEnrollmentStartAt(),
                semester.getEnrollmentEndAt(),
                semester.isCurrent()
        );
    }
}
