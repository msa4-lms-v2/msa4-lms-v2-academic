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
public class LeaveWithdrawalCancellationService {
    private static final String REASON = "자퇴 최종 승인으로 자동 취소되었습니다.";
    private final LeaveRequestRepository repository;
    private final LeaveAuditService audit;
    private final LeaveRequestPolicy policy;

    // 호출자는 자퇴 최종 승인 transaction에서 해당 학생 행을 먼저 잠가야 합니다.
    public void cancelPending(Long studentId, Long withdrawalId, CurrentUser actor, LeaveAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        for (var request : repository.findPendingForUpdate(studentId)) {
            var before = audit.snapshot(request);
            request.cancel(REASON);
            repository.flush();
            var after = audit.snapshot(request);
            after.put("withdrawalId", withdrawalId);
            audit.record(request.getId(), "LEAVE_REQUEST", before, after, "LEAVE_WITHDRAWAL_CANCELLED",
                    REASON, actor, context);
        }
    }
}
