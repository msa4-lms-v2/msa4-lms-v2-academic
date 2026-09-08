package com.msa4lmsv2academic.domain.grade.response;

import com.msa4lmsv2academic.domain.grade.entity.GradePointPolicy;
import com.msa4lmsv2academic.domain.grade.repository.StudentGradeQueryResult;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "학생의 공개 성적 상세")
public record StudentGradeItemResponseDTO(
        @Schema(description = "수강 ID", example = "301") Long enrollmentId,
        @Schema(description = "강의 ID", example = "101") Long classId,
        @Schema(description = "학년도", example = "2026") short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "학점", example = "3") byte credits,
        @Schema(description = "총점", example = "92.50") BigDecimal totalScore,
        @Schema(description = "등급", example = "A+") String letterGrade,
        @Schema(description = "등급 평점", example = "4.5") BigDecimal gradePoint,
        @Schema(description = "재수강을 고려한 GPA·학점 계산 반영 여부", example = "true") boolean reflectedInGpa
) {
    public static StudentGradeItemResponseDTO from(StudentGradeQueryResult result, boolean reflectedInGpa) {
        return new StudentGradeItemResponseDTO(
                result.enrollmentId(),
                result.classId(),
                result.academicYear(),
                result.term(),
                result.courseCode(),
                result.courseName(),
                result.credits(),
                result.totalScore(),
                result.letterGrade(),
                GradePointPolicy.pointOf(result.letterGrade()),
                reflectedInGpa
        );
    }
}
