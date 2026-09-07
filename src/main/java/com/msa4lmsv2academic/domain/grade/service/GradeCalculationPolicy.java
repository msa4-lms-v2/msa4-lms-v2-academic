package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.grade.request.GradeScoreRequestDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.global.error.InvalidGradeManagementRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class GradeCalculationPolicy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public GradeCalculationResult calculate(Lecture lecture, GradeScoreRequestDTO grade) {
        validateRatios(lecture);
        if (grade.midtermScore() == null || grade.finalScore() == null
                || grade.assignmentScore() == null || grade.attendanceScore() == null) {
            return new GradeCalculationResult(null, null);
        }

        BigDecimal total = weighted(grade.midtermScore(), lecture.getMidtermRatio())
                .add(weighted(grade.finalScore(), lecture.getFinalRatio()))
                .add(weighted(grade.assignmentScore(), lecture.getAssignmentRatio()))
                .add(weighted(grade.attendanceScore(), lecture.getAttendanceRatio()))
                .setScale(2, RoundingMode.HALF_UP);
        return new GradeCalculationResult(total, letterGrade(total));
    }

    private void validateRatios(Lecture lecture) {
        int totalRatio = lecture.getMidtermRatio() + lecture.getFinalRatio()
                + lecture.getAssignmentRatio() + lecture.getAttendanceRatio();
        if (totalRatio != 100) {
            throw new InvalidGradeManagementRequestException("강의의 성적 반영 비율 합계가 100이 아닙니다.");
        }
    }

    private BigDecimal weighted(BigDecimal score, int ratio) {
        return score.multiply(BigDecimal.valueOf(ratio)).divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    private String letterGrade(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(95)) >= 0) return "A+";
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (total.compareTo(BigDecimal.valueOf(85)) >= 0) return "B+";
        if (total.compareTo(BigDecimal.valueOf(80)) >= 0) return "B";
        if (total.compareTo(BigDecimal.valueOf(75)) >= 0) return "C+";
        if (total.compareTo(BigDecimal.valueOf(70)) >= 0) return "C";
        if (total.compareTo(BigDecimal.valueOf(65)) >= 0) return "D+";
        if (total.compareTo(BigDecimal.valueOf(60)) >= 0) return "D";
        return "F";
    }

    public record GradeCalculationResult(BigDecimal totalScore, String letterGrade) {
    }
}
