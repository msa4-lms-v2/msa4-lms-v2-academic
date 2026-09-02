package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.global.error.WithdrawalAccessDeniedException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class WithdrawalDismissalCancellationService {
    private static final String REASON = "관리자 제적 확정으로 자동 취소되었습니다.";
    private final WithdrawalRequestRepository repository;
    private final WithdrawalAuditService audit;
    private final AuditLogService auditLogService;

    // 호출자가 학생 행을 먼저 잠급니다. 자퇴 최종 승인과 동시에 종결 상태를 덮어쓰지 않습니다.
    public void cancelPending(Long studentId, Long dismissalId, User processor, CurrentUser actor,
                              LocalDateTime now, WithdrawalAuditContext context) {
        if (actor == null || !actor.isAdmin() || !processor.getId().equals(actor.id())) {
            throw new WithdrawalAccessDeniedException("관리자 제적 확정에서만 자동 취소할 수 있습니다.");
        }
        for (var request : repository.findActiveForUpdate(studentId,
                Set.of(WithdrawalStatus.PENDING, WithdrawalStatus.ADVISOR_APPROVED))) {
            var before = audit.snapshot(request);
            request.cancel(processor, REASON, now);
            repository.flush();
            var after = audit.snapshot(request);
            after.put("dismissalId", dismissalId);
            auditLogService.record(actor.id(), "WITHDRAWAL_DISMISSAL_CANCELLED", "WITHDRAWAL_REQUEST",
                    request.getId(), before, after, REASON, context.requestId(), context.ipAddress());
        }
    }
}
