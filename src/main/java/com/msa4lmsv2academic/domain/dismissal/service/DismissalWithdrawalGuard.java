package com.msa4lmsv2academic.domain.dismissal.service;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalStatus;
import com.msa4lmsv2academic.domain.dismissal.repository.DismissalCandidateRepository;
import com.msa4lmsv2academic.global.error.WithdrawalStateConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class DismissalWithdrawalGuard {
    private final DismissalCandidateRepository repository;

    // 자퇴 승인 서비스가 학생 행을 잠근 후 호출합니다.
    public void validateNoPendingCandidate(Long studentId) {
        if (repository.existsByStudentIdAndStatus(studentId, DismissalStatus.PENDING)) {
            throw new WithdrawalStateConflictException("대기 중인 제적 후보를 먼저 취소해야 자퇴를 최종 승인할 수 있습니다.");
        }
    }
}
