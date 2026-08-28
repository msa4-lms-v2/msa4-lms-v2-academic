package com.msa4lmsv2academic.domain.withdrawal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.leaverequest.service.LeaveWithdrawalCancellationService;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalQueryRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.domain.withdrawal.request.FinalWithdrawalReviewRequestDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRepository;
    @Mock
    private AcademicStatusHistoryRepository historyRepository;
    @Mock
    private WithdrawalQueryRepository queryRepository;
    @Mock
    private WithdrawalIdempotencyService idempotencyService;
    @Mock
    private AuditLogService auditLogService;

    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalService(
                withdrawalRepository,
                historyRepository,
                queryRepository,
                idempotencyService,
                new WithdrawalPolicy(),
                new WithdrawalAuditService(auditLogService),
                mock(LeaveWithdrawalCancellationService.class)
        );
    }

    @Test
    void adminApprovalChangesStudentStatusAndCreatesHistoryInSameServiceCall() {
        User studentUser = user(21L, "학생");
        User advisorUser = user(11L, "지도교수");
        User adminUser = user(1L, "관리자");
        Student student = mock(Student.class);
        lenient().when(student.getId()).thenReturn(41L);
        lenient().when(student.getUser()).thenReturn(studentUser);
        when(student.getAcademicStatus()).thenReturn(AcademicStatus.ENROLLED);

        WithdrawalRequest withdrawal = WithdrawalRequest.create(
                student, "개인 사유", LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1), studentUser
        );
        withdrawal.advisorApprove(advisorUser, LocalDateTime.now());
        when(withdrawalRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.findStudentIdById(51L)).thenReturn(Optional.of(41L));
        when(queryRepository.findStudentByIdForUpdate(41L)).thenReturn(Optional.of(student));
        when(queryRepository.findUserById(1L)).thenReturn(Optional.of(adminUser));
        when(withdrawalRepository.saveAndFlush(withdrawal)).thenReturn(withdrawal);

        var response = service.reviewByAdmin(
                51L,
                new FinalWithdrawalReviewRequestDTO(true, LocalDate.now(java.time.ZoneId.of("Asia/Seoul")), null),
                "review-key", new CurrentUser(1L, "ADMIN"), new WithdrawalAuditContext(null, null)
        );

        assertThat(response.status()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(response.effectiveDate()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        verify(student).changeAcademicStatus(AcademicStatus.WITHDRAWN);
        verify(withdrawalRepository).saveAndFlush(withdrawal);
        verify(historyRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private User user(Long id, String name) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getName()).thenReturn(name);
        return user;
    }
}
