package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferPeriodSaveRequestDTO;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferCreateRequestDTO;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class DepartmentTransferPolicy {
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void requireReader(CurrentUser actor) {
        if (actor == null || actor.id() == null || !("STUDENT".equals(actor.role()) || actor.isAdmin())) {
            throw new DepartmentTransferAccessDeniedException("학생 본인 또는 관리자만 접근할 수 있습니다.");
        }
    }

    public void requireRole(CurrentUser actor, String role) {
        requireReader(actor);
        if (!role.equals(actor.role())) {
            throw new DepartmentTransferAccessDeniedException("요청을 처리할 역할이 아닙니다.");
        }
    }

    public void requireId(Long id) {
        if (id == null || id <= 0) throw new InvalidDepartmentTransferRequestException("양수 ID가 필요합니다.");
    }

    public void requireEnrolled(AcademicStatus status) {
        if (status != AcademicStatus.ENROLLED) {
            throw new DepartmentTransferConflictException("재학 상태인 학생만 전과를 신청하거나 승인받을 수 있습니다.");
        }
    }

    public void requirePending(AcademicChangeRequest request) {
        if (request.getStatus() != AcademicChangeRequestStatus.PENDING) {
            throw new DepartmentTransferConflictException("대기 중인 전과 신청만 처리할 수 있습니다.");
        }
    }

    public void validateCreate(DepartmentTransferCreateRequestDTO body) {
        if (body == null || body.targetDepartmentId() == null || body.targetSemesterId() == null) {
            throw new InvalidDepartmentTransferRequestException("희망 학과와 적용 학기가 필요합니다.");
        }
        requireId(body.targetDepartmentId());
        requireId(body.targetSemesterId());
    }

    public void validatePeriod(DepartmentTransferPeriodSaveRequestDTO body) {
        if (body == null || body.semesterId() == null || body.startAt() == null || body.endAt() == null
                || body.active() == null || !body.isPeriodOrderValid()) {
            throw new InvalidDepartmentTransferRequestException("학기·활성 여부와 올바른 접수 기간이 필요합니다.");
        }
        requireId(body.semesterId());
        requiredReason(body.reason(), 255);
    }

    public String requiredReason(String reason, int max) {
        if (reason == null || reason.isBlank() || reason.length() > max) {
            throw new InvalidDepartmentTransferRequestException("사유는 공백이 아닌 1~" + max + "자여야 합니다.");
        }
        return reason.strip();
    }
}
