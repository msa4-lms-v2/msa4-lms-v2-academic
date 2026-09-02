package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.repository.LeaveRequestRepository;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LeaveDismissalCancellationService {
    private static final String REASON = "관리자 제적 확정으로 자동 취소되었습니다.";
    private final LeaveRequestRepository repository;
    private final LeaveAuditService audit;
    private final LeaveRequestPolicy policy;

    // 제적 서비스의 학생 행 잠금과 동일 transaction을 사용합니다. 외부 취소 API 권한은 바꾸지 않습니다.
    public void cancelPending(Long studentId, Long dismissalId, CurrentUser actor, LeaveAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        for (var request : repository.findPendingForUpdate(studentId)) {
            var before = audit.snapshot(request);
            request.cancel(REASON);
            repository.flush();
            var after = audit.snapshot(request);
            after.put("dismissalId", dismissalId);
            audit.record(request.getId(), "LEAVE_REQUEST", before, after, "LEAVE_DISMISSAL_CANCELLED",
                    REASON, actor, context);
        }
    }
}
