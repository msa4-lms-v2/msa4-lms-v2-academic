package com.msa4lmsv2academic.domain.academicschedule.response;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학사일정 분류별 기본 문구")
public record AcademicScheduleTemplateResponseDTO(
        @Schema(description = "일정 분류 코드", example = "ENROLLMENT") AcademicScheduleCategory category,
        @Schema(description = "일정 분류명", example = "수강신청") String categoryLabel,
        @Schema(description = "기본 일정명", example = "수강신청 안내") String defaultTitle,
        @Schema(description = "기본 일정 내용", example = "수강신청 기간입니다. 기간 내에 수강신청을 완료해 주세요.")
        String defaultContent
) {
    public static AcademicScheduleTemplateResponseDTO from(AcademicScheduleCategory category) {
        return new AcademicScheduleTemplateResponseDTO(
                category,
                category.getLabel(),
                category.getDefaultTitle(),
                category.getDefaultContent()
        );
    }
}
