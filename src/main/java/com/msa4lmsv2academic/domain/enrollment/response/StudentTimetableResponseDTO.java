package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "학생 본인 학기 시간표 조회 결과")
public record StudentTimetableResponseDTO(
        @Schema(description = "조회 학년도", example = "2026") Short academicYear,
        @Schema(description = "조회 학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "활성 수강 강의의 총 신청학점", example = "15") int totalCredits,
        @Schema(description = "시간표 강의 목록. 수강 강의가 없으면 빈 목록")
        List<StudentTimetableEntryResponseDTO> items
) {

    public static StudentTimetableResponseDTO from(
            Short academicYear,
            SemesterTerm term,
            List<StudentTimetableEntryResponseDTO> items
    ) {
        int totalCredits = items.stream().mapToInt(item -> item.credits()).sum();
        return new StudentTimetableResponseDTO(academicYear, term, totalCredits, List.copyOf(items));
    }
}
