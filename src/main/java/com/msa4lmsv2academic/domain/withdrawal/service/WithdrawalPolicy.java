package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.InvalidWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.WithdrawalStateConflictException;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalPolicy {
    public void validateRequestedDate(LocalDate requestedDate, LocalDate today) {
        if (requestedDate != null && requestedDate.isBefore(today)) {
            throw new InvalidWithdrawalRequestException("희망 적용일은 신청 당일 또는 미래 날짜여야 합니다.");
        }
    }

    public void validateFinalDate(LocalDate effectiveDate, LocalDate requestedDate, LocalDate today) {
        if (effectiveDate == null || !effectiveDate.equals(today)) {
            throw new InvalidWithdrawalRequestException("최종 적용일은 승인 당일(KST)이어야 합니다.");
        }
        if (requestedDate != null && today.isBefore(requestedDate)) {
            throw new WithdrawalStateConflictException("희망 적용일 이전에는 최종 승인할 수 없습니다.");
        }
    }

    public void validateAcademicStatus(AcademicStatus status) {
        if (status != AcademicStatus.ENROLLED && status != AcademicStatus.ON_LEAVE) {
            throw new WithdrawalStateConflictException("재학 또는 휴학 상태의 학생만 자퇴 처리할 수 있습니다.");
        }
    }
}

