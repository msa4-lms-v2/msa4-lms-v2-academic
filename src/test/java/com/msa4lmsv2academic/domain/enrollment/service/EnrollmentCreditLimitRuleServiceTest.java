package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleStatusRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCreditLimitRuleResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.global.error.DuplicateEnrollmentCreditLimitRuleException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitRuleStateConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentCreditLimitRuleServiceTest {

    private static final long SEMESTER_ID = 12L;
    private static final long RULE_ID = 5L;
    private static final CurrentUser ADMIN = new CurrentUser(3L, "ADMIN");

    private EnrollmentCreditLimitRuleRepository ruleRepository;
    private EnrollmentCreditLimitRuleQueryRepository queryRepository;
    private SemesterRepository semesterRepository;
    private AuditLogService auditLogService;
    private EnrollmentCreditLimitRuleService service;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(EnrollmentCreditLimitRuleRepository.class);
        queryRepository = mock(EnrollmentCreditLimitRuleQueryRepository.class);
        semesterRepository = mock(SemesterRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new EnrollmentCreditLimitRuleService(
                ruleRepository,
                queryRepository,
                semesterRepository,
                auditLogService
        );
        when(ruleRepository.saveAndFlush(any(EnrollmentCreditLimitRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsActiveRuleForFutureEnrollmentPeriodAndAudits() {
        Semester semester = semester(LocalDateTime.now().plusDays(10));
        when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));
        when(ruleRepository.existsBySemesterId(SEMESTER_ID)).thenReturn(false);

        EnrollmentCreditLimitRuleResponseDTO response = service.create(
                new EnrollmentCreditLimitRuleCreateRequestDTO(SEMESTER_ID, 18, "기준 확정"),
                ADMIN,
                "request-1",
                "127.0.0.1"
        );

        assertThat(response.maxCredits()).isEqualTo(18);
        assertThat(response.active()).isTrue();
        verify(ruleRepository).saveAndFlush(any(EnrollmentCreditLimitRule.class));
        verify(auditLogService).record(
                eq(ADMIN.id()),
                eq("ENROLLMENT_CREDIT_LIMIT_RULE_CREATE"),
                eq("ENROLLMENT_CREDIT_LIMIT_RULE"),
                isNull(),
                isNull(),
                anyMap(),
                eq("기준 확정"),
                eq("request-1"),
                eq("127.0.0.1")
        );
    }

    @Test
    void rejectsDuplicateRuleEvenWhenExistingRuleCouldBeInactive() {
        Semester semester = semester(LocalDateTime.now().plusDays(10));
        when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));
        when(ruleRepository.existsBySemesterId(SEMESTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new EnrollmentCreditLimitRuleCreateRequestDTO(SEMESTER_ID, 18, "중복"),
                ADMIN,
                null,
                null
        )).isInstanceOf(DuplicateEnrollmentCreditLimitRuleException.class);

        verify(ruleRepository, never()).saveAndFlush(any());
        verify(auditLogService, never()).record(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void rejectsMutationAfterEnrollmentStart() {
        Semester semester = semester(LocalDateTime.now().minusMinutes(1));
        when(semesterRepository.findById(SEMESTER_ID)).thenReturn(Optional.of(semester));

        assertThatThrownBy(() -> service.create(
                new EnrollmentCreditLimitRuleCreateRequestDTO(SEMESTER_ID, 18, "기간 검증"),
                ADMIN,
                null,
                null
        )).isInstanceOf(EnrollmentCreditLimitRuleStateConflictException.class);
    }

    @Test
    void updatesOnlyMaxCreditsBeforeEnrollmentStart() {
        Semester semester = semester(LocalDateTime.now().plusDays(10));
        EnrollmentCreditLimitRule rule = EnrollmentCreditLimitRule.create(semester, 18);
        when(queryRepository.findByIdWithSemester(RULE_ID)).thenReturn(Optional.of(rule));

        EnrollmentCreditLimitRuleResponseDTO response = service.update(
                RULE_ID,
                new EnrollmentCreditLimitRuleUpdateRequestDTO(21, "상한 변경"),
                ADMIN,
                "request-2",
                "127.0.0.1"
        );

        assertThat(response.semesterId()).isEqualTo(SEMESTER_ID);
        assertThat(response.maxCredits()).isEqualTo(21);
        verify(auditLogService).record(
                eq(ADMIN.id()),
                eq("ENROLLMENT_CREDIT_LIMIT_RULE_UPDATE"),
                eq("ENROLLMENT_CREDIT_LIMIT_RULE"),
                isNull(),
                anyMap(),
                anyMap(),
                eq("상한 변경"),
                eq("request-2"),
                eq("127.0.0.1")
        );
    }

    @Test
    void deactivatesRuleWithoutDeletingIt() {
        Semester semester = semester(LocalDateTime.now().plusDays(10));
        EnrollmentCreditLimitRule rule = EnrollmentCreditLimitRule.create(semester, 18);
        when(queryRepository.findByIdWithSemester(RULE_ID)).thenReturn(Optional.of(rule));

        EnrollmentCreditLimitRuleResponseDTO response = service.changeStatus(
                RULE_ID,
                new EnrollmentCreditLimitRuleStatusRequestDTO(false, "운영 계획 변경"),
                ADMIN,
                "request-3",
                "127.0.0.1"
        );

        assertThat(response.active()).isFalse();
        verify(ruleRepository).saveAndFlush(rule);
        verify(ruleRepository, never()).delete(any());
    }

    @Test
    void rejectsNonAdminManagement() {
        assertThatThrownBy(() -> service.create(
                new EnrollmentCreditLimitRuleCreateRequestDTO(SEMESTER_ID, 18, "권한 검증"),
                new CurrentUser(1L, "STUDENT"),
                null,
                null
        )).isInstanceOf(EnrollmentCreditLimitRuleAccessDeniedException.class);
    }

    private Semester semester(LocalDateTime enrollmentStartAt) {
        Semester semester = mock(Semester.class);
        when(semester.getId()).thenReturn(SEMESTER_ID);
        when(semester.getAcademicYear()).thenReturn((short) 2027);
        when(semester.getTerm()).thenReturn(SemesterTerm.FIRST);
        when(semester.getStartDate()).thenReturn(LocalDate.of(2027, 3, 2));
        when(semester.getEndDate()).thenReturn(LocalDate.of(2027, 6, 18));
        when(semester.getEnrollmentStartAt()).thenReturn(enrollmentStartAt);
        when(semester.getEnrollmentEndAt()).thenReturn(enrollmentStartAt.plusDays(5));
        return semester;
    }
}
