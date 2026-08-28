package com.msa4lmsv2academic.domain.leaverequest.response;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestPeriod;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record LeavePeriodResponseDTO(
        @Schema(description = "기간 설정 ID", example = "1") Long id,
        @Schema(description = "적용 학기 ID", example = "23") Long semesterId,
        @Schema(description = "적용 학년도", example = "2027") short academicYear,
        @Schema(description = "적용 학기 구분", example = "FIRST") SemesterTerm term,
        @Schema(description = "신청 유형", example = "GENERAL_LEAVE") LeaveRequestType requestType,
        @Schema(description = "접수 시작(KST)", example = "2026-12-01T09:00:00") LocalDateTime startAt,
        @Schema(description = "접수 종료(KST)", example = "2026-12-31T18:00:00") LocalDateTime endAt,
        @Schema(description = "승인 허용 시작(KST)", example = "2027-03-02T09:00:00") LocalDateTime approvalStartAt,
        @Schema(description = "승인 허용 종료(KST)", example = "2027-03-10T18:00:00") LocalDateTime approvalEndAt,
        @Schema(description = "설정 활성 여부", example = "true") boolean active,
        @Schema(description = "접수 가능 기간 여부. 학생 개인의 학적·중복·증빙 조건 통과를 뜻하지 않음", example = "false") boolean open,
        @Schema(description = "설정 생성 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime createdAt,
        @Schema(description = "설정 수정 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime updatedAt
) {
    public static LeavePeriodResponseDTO from(LeaveRequestPeriod period, LocalDateTime now) {
        return new LeavePeriodResponseDTO(period.getId(), period.getSemester().getId(),
                period.getSemester().getAcademicYear(), period.getSemester().getTerm(), period.getRequestType(),
                period.getStartAt(), period.getEndAt(), period.getApprovalStartAt(), period.getApprovalEndAt(),
                period.isActive(), period.accepts(now), period.getCreatedAt(), period.getUpdatedAt());
    }
}
