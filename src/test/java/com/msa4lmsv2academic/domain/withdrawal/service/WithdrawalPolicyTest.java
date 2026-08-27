package com.msa4lmsv2academic.domain.withdrawal.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.InvalidWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.WithdrawalStateConflictException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WithdrawalPolicyTest {
    private final WithdrawalPolicy policy = new WithdrawalPolicy();
    private final LocalDate today = LocalDate.of(2026, 9, 2);

    @Test
    void requestedDateIsOptionalAndAcceptsTodayAndFuture() {
        assertThatCode(() -> policy.validateRequestedDate(null, today)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateRequestedDate(today, today)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateRequestedDate(today.plusDays(1), today)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateRequestedDate(today.minusDays(1), today))
                .isInstanceOf(InvalidWithdrawalRequestException.class);
    }

    @Test
    void finalDateMustBeTodayWithoutRetroactiveOrScheduledApproval() {
        for (LocalDate date : new LocalDate[]{null, today.minusDays(1), today.plusDays(1)}) {
            assertThatThrownBy(() -> policy.validateFinalDate(date, null, today))
                    .isInstanceOf(InvalidWithdrawalRequestException.class);
        }
    }

    @Test
    void earlierThanRequestedDateIsBlockedButLaterApprovalIsNotBackdated() {
        assertThatThrownBy(() -> policy.validateFinalDate(today, today.plusDays(1), today))
                .isInstanceOf(WithdrawalStateConflictException.class);
        assertThatCode(() -> policy.validateFinalDate(today, today, today)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateFinalDate(today, today.minusDays(1), today)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateFinalDate(today, null, today)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = AcademicStatus.class, names = {"WITHDRAWN", "GRADUATED", "DISMISSED"})
    void terminalAcademicStateIsBlocked(AcademicStatus status) {
        assertThatThrownBy(() -> policy.validateAcademicStatus(status)).isInstanceOf(WithdrawalStateConflictException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AcademicStatus.class, names = {"ENROLLED", "ON_LEAVE"})
    void enrolledAndOnLeaveAreEligible(AcademicStatus status) {
        assertThatCode(() -> policy.validateAcademicStatus(status)).doesNotThrowAnyException();
    }
}

