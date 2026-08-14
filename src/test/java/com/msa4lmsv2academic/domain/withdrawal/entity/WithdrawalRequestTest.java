package com.msa4lmsv2academic.domain.withdrawal.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WithdrawalRequestTest {

    @Test
    void advisorApprovalMustPrecedeAdminApproval() {
        User studentUser = mock(User.class);
        User advisorUser = mock(User.class);
        User adminUser = mock(User.class);
        WithdrawalRequest request = WithdrawalRequest.create(
                mock(Student.class), "개인 사유", LocalDate.of(2026, 9, 1), studentUser
        );

        request.advisorApprove(advisorUser, LocalDateTime.of(2026, 8, 20, 9, 0));
        request.approve(adminUser, LocalDate.of(2026, 9, 2), LocalDateTime.of(2026, 8, 21, 10, 0));

        assertThat(request.getStatus()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(request.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(request.getAdvisorReviewedBy()).isSameAs(advisorUser);
        assertThat(request.getProcessedBy()).isSameAs(adminUser);
    }

    @Test
    void adminCannotApprovePendingRequestDirectly() {
        WithdrawalRequest request = WithdrawalRequest.create(
                mock(Student.class), "개인 사유", null, mock(User.class)
        );

        assertThatThrownBy(() -> request.approve(
                mock(User.class), LocalDate.of(2026, 9, 2), LocalDateTime.now()
        )).isInstanceOf(IllegalStateException.class);
    }
}
