package com.msa4lmsv2academic.domain.evaluation.response;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "교수 담당 강의의 익명 강의평가 결과")
public record ProfessorLectureEvaluationResponseDTO(
        @Schema(description = "강의 ID", example = "21")
        Long lectureId,
        @Schema(description = "과목 코드", example = "CSE301")
        String courseCode,
        @Schema(description = "과목명", example = "소프트웨어공학")
        String courseName,
        @Schema(description = "분반", example = "01")
        String sectionNo,
        @Schema(description = "학년도", example = "2026")
        Short academicYear,
        @Schema(description = "학기", example = "FIRST")
        SemesterTerm term,
        @Schema(description = "현재 수강 인원", example = "40")
        long activeEnrollmentCount,
        @Schema(description = "강의평가 응답 수", example = "32")
        long responseCount,
        @Schema(description = "강의평가 응답률(%)", example = "80.00")
        BigDecimal responseRate,
        @Schema(description = "응답 존재 여부", example = "true")
        boolean hasResponses,
        @Schema(description = "전체 문항 평균. 응답이 없으면 null", example = "4.25", nullable = true)
        BigDecimal overallAverage,
        @Schema(description = "문항 코드별 평균 점수", example = "{\"CONTENT_QUALITY\":4.30}")
        Map<String, BigDecimal> questionAverages,
        @Schema(description = "학생 식별정보가 포함되지 않은 서술형 의견 목록")
        List<String> comments
) {
}
