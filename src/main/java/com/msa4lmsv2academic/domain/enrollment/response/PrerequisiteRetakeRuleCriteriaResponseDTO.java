package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "선수과목 기준정보")
public record PrerequisiteRetakeRuleCriteriaResponseDTO(
        @Schema(description = "기준 ID", example = "4") Long ruleId,
        @Schema(description = "대상 교과목 ID", example = "20") Long courseId,
        @Schema(description = "대상 교과목 코드", example = "CSE3001") String courseCode,
        @Schema(description = "대상 교과목명", example = "운영체제") String courseName,
        @Schema(description = "대상 교과목 소유 학과 ID", example = "130") Long departmentId,
        @Schema(description = "대상 교과목 소유 학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "선수 교과목 ID", example = "10") Long prerequisiteCourseId,
        @Schema(description = "선수 교과목 코드", example = "CSE2001") String prerequisiteCourseCode,
        @Schema(description = "선수 교과목명", example = "자료구조") String prerequisiteCourseName,
        @Schema(description = "기준 활성 여부", example = "true") boolean active,
        @Schema(description = "등록 시각", example = "2026-08-24T15:00:00") LocalDateTime createdAt,
        @Schema(description = "최종 수정 시각", example = "2026-08-24T15:00:00") LocalDateTime updatedAt
) {

    public static PrerequisiteRetakeRuleCriteriaResponseDTO from(CoursePrerequisite rule) {
        return new PrerequisiteRetakeRuleCriteriaResponseDTO(
                rule.getId(),
                rule.getCourse().getId(),
                rule.getCourse().getCode(),
                rule.getCourse().getName(),
                rule.getCourse().getDepartment().getId(),
                rule.getCourse().getDepartment().getName(),
                rule.getPrerequisiteCourse().getId(),
                rule.getPrerequisiteCourse().getCode(),
                rule.getPrerequisiteCourse().getName(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
