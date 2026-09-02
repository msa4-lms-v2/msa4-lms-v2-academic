package com.msa4lmsv2academic.domain.graduation.response;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditExclusionReason;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditRecordResult;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "졸업요건 진단에 사용되는 과목별 수강·성적 근거")
public record GraduationCreditRecordResponseDTO(
        @Schema(description = "수강 ID", example = "501") Long enrollmentId,
        @Schema(description = "교과목 ID", example = "31") Long courseId,
        @Schema(description = "교과목 코드", example = "CSE301") String courseCode,
        @Schema(description = "교과목명", example = "운영체제") String courseName,
        @Schema(description = "교과목 학점", example = "3") byte credits,
        @Schema(description = "이수 구분", example = "MAJOR_REQUIRED") CompletionType completionType,
        @Schema(description = "학년도", example = "2024") short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "수강 상태", example = "ACTIVE") EnrollmentStatus enrollmentStatus,
        @Schema(description = "성적 공개 상태", example = "OPENED") GradeStatus gradeStatus,
        @Schema(description = "공개된 문자 성적. DRAFT이면 역할과 관계없이 null", example = "A",
                nullable = true)
        String letterGrade,
        @Schema(description = "졸업학점 반영 결과", example = "APPLIED")
        GraduationCreditRecordResult result,
        @Schema(description = "실제 반영 학점. 제외 기록은 0", example = "3") int appliedCredits,
        @Schema(description = "표준 제외 사유 코드. 반영 기록은 null", example = "FAILED_GRADE",
                nullable = true)
        GraduationCreditExclusionReason exclusionReason,
        @Schema(description = "사용자용 제외 사유 설명. 반영 기록은 null",
                example = "F 성적은 졸업학점에 반영되지 않습니다.", nullable = true)
        String exclusionMessage
) {
}
