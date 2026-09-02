package com.msa4lmsv2academic.domain.doublemajor.service;

import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class DoubleMajorPolicy {
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void requireReader(CurrentUser actor) {
        if (actor == null || actor.id() == null || !("STUDENT".equals(actor.role()) || actor.isAdmin())) {
            throw new DoubleMajorAccessDeniedException("학생 본인 또는 관리자만 접근할 수 있습니다.");
        }
    }

    public void requireRole(CurrentUser actor, String role) {
        requireReader(actor);
        if (!role.equals(actor.role())) throw new DoubleMajorAccessDeniedException("요청을 처리할 역할이 아닙니다.");
    }

    public void requireId(Long id) {
        if (id == null || id <= 0) throw new InvalidDoubleMajorRequestException("양수 ID가 필요합니다.");
    }

    public void requireEnrolled(AcademicStatus status) {
        if (status != AcademicStatus.ENROLLED) {
            throw new DoubleMajorConflictException("재학 상태인 학생만 복수전공을 신청하거나 승인받을 수 있습니다.");
        }
    }

    public void requirePending(AcademicChangeRequest request) {
        if (request.getStatus() != AcademicChangeRequestStatus.PENDING) {
            throw new DoubleMajorConflictException("대기 중인 복수전공 신청만 처리할 수 있습니다.");
        }
    }

    public void validateCreate(DoubleMajorCreateRequestDTO body) {
        if (body == null || body.targetDepartmentId() == null) {
            throw new InvalidDoubleMajorRequestException("희망 복수전공이 필요합니다.");
        }
        requireId(body.targetDepartmentId());
    }

    public void validatePeriod(DoubleMajorPeriodSaveRequestDTO body) {
        if (body == null || body.semesterId() == null || body.startAt() == null || body.endAt() == null
                || body.active() == null || !body.isPeriodOrderValid()) {
            throw new InvalidDoubleMajorRequestException("기준 학기·활성 여부와 올바른 접수 기간이 필요합니다.");
        }
        requireId(body.semesterId());
        requiredReason(body.reason(), 255);
    }

    public String requiredReason(String reason, int max) {
        if (reason == null || reason.isBlank() || reason.length() > max) {
            throw new InvalidDoubleMajorRequestException("사유는 공백이 아닌 1~" + max + "자여야 합니다.");
        }
        return reason.strip();
    }
}
