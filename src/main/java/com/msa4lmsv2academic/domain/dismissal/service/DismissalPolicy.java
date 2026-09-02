package com.msa4lmsv2academic.domain.dismissal.service;

import com.msa4lmsv2academic.domain.dismissal.entity.*;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class DismissalPolicy {
    public void requireAdmin(CurrentUser actor) {
        if (actor == null || actor.id() == null || actor.id() <= 0 || !actor.isAdmin()) {
            throw new DismissalAccessDeniedException("관리자만 제적 후보를 처리·조회할 수 있습니다.");
        }
    }

    public void validateAcademicStatus(AcademicStatus status, DismissalReasonType reasonType) {
        if (status != AcademicStatus.ENROLLED && status != AcademicStatus.ON_LEAVE) {
            throw new DismissalConflictException("재학 또는 휴학 상태인 학생만 제적 처리할 수 있습니다.");
        }
        if (reasonType == DismissalReasonType.LEAVE_EXPIRED && status != AcademicStatus.ON_LEAVE) {
            throw new DismissalConflictException("휴학만료제적은 현재 휴학 중인 학생만 처리할 수 있습니다.");
        }
    }

    public void validateVersion(DismissalCandidate candidate, Long version) {
        if (version == null || version < 0) throw new InvalidDismissalRequestException("조회한 version이 필요합니다.");
        if (candidate.getVersion() != version) throw new DismissalConflictException("내용이 변경되었습니다. 최신 제적 후보를 다시 확인해 주세요.");
        candidate.requirePending();
    }

    public void validateReason(DismissalReasonType type, String reason) {
        if (type == null) throw new InvalidDismissalRequestException("제적 사유 종류가 필요합니다.");
        requireReason(reason);
    }

    public void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new InvalidDismissalRequestException("1~500자의 사유가 필요합니다.");
        }
    }

    public static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0); }
}
