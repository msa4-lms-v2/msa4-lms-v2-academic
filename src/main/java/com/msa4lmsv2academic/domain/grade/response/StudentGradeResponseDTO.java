package com.msa4lmsv2academic.domain.grade.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "학생 본인 성적 조회 결과")
public record StudentGradeResponseDTO(
        @Schema(description = "전체 GPA", example = "3.75") BigDecimal totalGpa,
        @Schema(description = "전체 GPA 계산 반영 학점", example = "18") int totalCredits,
        @Schema(description = "현재 조회 조건의 GPA", example = "4.00") BigDecimal queryGpa,
        @Schema(description = "현재 조회 조건의 GPA 계산 반영 학점", example = "6") int queryCredits,
        @Schema(description = "조회된 공개 성적 목록") List<StudentGradeItemResponseDTO> grades
) {
}
