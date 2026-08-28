package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.*;
import com.msa4lmsv2academic.domain.leaverequest.request.*;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestPolicy {
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void requireReader(CurrentUser actor) {
        if (actor == null || actor.id() == null || !("STUDENT".equals(actor.role()) || actor.isAdmin())) {
            throw new LeaveRequestAccessDeniedException("학생 본인 또는 관리자만 접근할 수 있습니다.");
        }
    }

    public void requireRole(CurrentUser actor, String role) {
        requireReader(actor);
        if (!role.equals(actor.role())) throw new LeaveRequestAccessDeniedException("요청을 처리할 역할이 아닙니다.");
    }

    public void requireId(Long id) {
        if (id == null || id <= 0) throw new InvalidLeaveRequestException("양수 ID가 필요합니다.");
    }

    public String requiredReason(String reason, int max) {
        if (reason == null || reason.isBlank() || reason.length() > max) {
            throw new InvalidLeaveRequestException("사유는 공백이 아닌 1~" + max + "자여야 합니다.");
        }
        return reason;
    }

    public int termIndex(short year, byte term) {
        if (year < 1 || term < 1 || term > 2) throw new InvalidLeaveRequestException("유효한 학년도와 1·2학기가 필요합니다.");
        return year * 2 + term - 1;
    }

    public void validateCreate(LeaveRequestCreateRequestDTO body) {
        if (body == null || body.requestType() == null || body.targetYear() == null || body.targetSemester() == null) {
            throw new InvalidLeaveRequestException("신청 유형과 적용 학기가 필요합니다.");
        }
        termIndex(body.targetYear(), body.targetSemester());
        if (body.reason() != null && body.reason().length() > 500) throw new InvalidLeaveRequestException("사유는 500자 이하여야 합니다.");
        if (body.requestType() == LeaveRequestType.GENERAL_LEAVE) {
            requiredReason(body.reason(), 500);
            if (body.returnYear() == null || body.returnSemester() == null
                    || termIndex(body.returnYear(), body.returnSemester()) <= termIndex(body.targetYear(), body.targetSemester())) {
                throw new InvalidLeaveRequestException("복학 예정은 휴학 시작보다 최소 한 학기 뒤여야 합니다.");
            }
        } else if (body.returnYear() != null || body.returnSemester() != null) {
            throw new InvalidLeaveRequestException("군휴학·복학 신청에는 복학 예정 값을 직접 지정하지 않습니다.");
        }
    }

    public void validateAcademicStatus(AcademicStatus status, LeaveRequestType type) {
        AcademicStatus expected = type.isLeave() ? AcademicStatus.ENROLLED : AcademicStatus.ON_LEAVE;
        if (status != expected) throw new LeaveRequestConflictException(
                type.isLeave() ? "재학 상태에서만 휴학을 신청·승인할 수 있습니다." : "휴학 상태에서만 복학을 신청·승인할 수 있습니다.");
    }

    public void requirePending(LeaveRequest request) {
        if (request.getStatus() != LeaveRequestStatus.PENDING) throw new LeaveRequestConflictException("대기 중인 신청만 변경할 수 있습니다.");
    }

    public void validatePeriod(LeavePeriodSaveRequestDTO body) {
        if (body == null || body.semesterId() == null || body.requestType() == null
                || body.startAt() == null || body.endAt() == null || body.approvalStartAt() == null
                || body.approvalEndAt() == null || body.active() == null || !body.isPeriodOrderValid()) {
            throw new InvalidLeaveRequestException("학기·유형·활성 여부와 올바른 접수·승인 기간이 필요합니다.");
        }
        requireId(body.semesterId());
        requiredReason(body.reason(), 255);
    }
}
