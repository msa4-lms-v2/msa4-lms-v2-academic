package com.msa4lmsv2academic.domain.admission.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.msa4lmsv2academic.domain.admission.entity.*;
import com.msa4lmsv2academic.domain.admission.repository.*;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentQueryRepository;
import com.msa4lmsv2academic.domain.outbox.entity.*;
import com.msa4lmsv2academic.domain.outbox.repository.OutboxEventRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.user.entity.*;
import com.msa4lmsv2academic.domain.user.service.UserQueryService;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdmissionProvisioningManagementTest {
    @Mock AdmissionCandidateRepository admissionCandidateRepository;
    @Mock AdmissionCandidateQueryRepository admissionCandidateQueryRepository;
    @Mock DepartmentQueryRepository departmentQueryRepository;
    @Mock ProfessorRepository professorRepository;
    @Mock UserQueryService userQueryService;
    @Mock AuditLogService auditLogService;
    @Mock OutboxEventService outboxEventService;
    @Mock OutboxEventRepository outboxEventRepository;
    @InjectMocks AdmissionCandidateService service;
    final CurrentUser admin = new CurrentUser(1L, "ADMIN");

    AdmissionCandidate candidate() {
        var department = Department.create("01", null, "학과", true);
        ReflectionTestUtils.setField(department, "id", 1L);
        var user = User.provision(1L, "관리자", "admin@example.com", null, null, UserRole.ADMIN);
        var candidate = AdmissionCandidate.create("학생", LocalDate.of(2008, 1, 1), "s@example.com",
                null, null, department, (short) 2026, user);
        ReflectionTestUtils.setField(candidate, "id", 7L);
        when(admissionCandidateRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(candidate));
        return candidate;
    }

    @Test void retryReusesOriginalPayloadAndStopsOldWork() {
        candidate();
        var payload = Map.<String,Object>of("admissionCandidateId", 7L, "advisorProfessorId", 3L);
        var event = OutboxEvent.create("ADMISSION_CANDIDATE", 7L, "AdmissionCandidateRegistered", payload, 1L);
        when(outboxEventRepository.lockAdmissionRequests(7L)).thenReturn(List.of(event));
        service.manageProvisioning(7L, false, admin, null, null);
        assertThat(event.getLastErrorCode()).isEqualTo("SUPERSEDED_BY_RETRY");
        verify(outboxEventService).record(eq("ADMISSION_CANDIDATE"), eq(7L), eq("AdmissionCandidateRetryRequested"),
                argThat(p -> p.get("advisorProfessorId").equals(3L) && p.get("administratorId").equals(1L)), eq(1L));
    }

    @Test void cancelStopsPendingWorkAndQueuesAuthCleanup() {
        var candidate = candidate();
        when(userQueryService.findById(1L)).thenReturn(Optional.of(candidate.getCreatedBy()));
        var event = OutboxEvent.create("ADMISSION_CANDIDATE", 7L, "AdmissionCandidateRegistered", Map.of(), 1L);
        when(outboxEventRepository.lockAdmissionRequests(7L)).thenReturn(List.of(event));
        service.manageProvisioning(7L, true, admin, null, null);
        assertThat(candidate.getStatus()).isEqualTo(AdmissionCandidateStatus.CANCELLED);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(event.getLastErrorCode()).isEqualTo("CANCELLED_BY_ADMIN");
        verify(outboxEventService).record(eq("ADMISSION_CANDIDATE"), eq(7L), eq("AdmissionCandidateCancelled"), anyMap(), eq(1L));
    }

    @Test void completedCandidateCannotBeRetriedOrCancelled() {
        var candidate = candidate();
        ReflectionTestUtils.setField(candidate, "status", AdmissionCandidateStatus.PROVISIONED);
        assertThatThrownBy(() -> service.manageProvisioning(7L, true, admin, null, null))
                .isInstanceOf(AdmissionCandidateStateConflictException.class);
        assertThatThrownBy(() -> service.manageProvisioning(7L, false, admin, null, null))
                .isInstanceOf(AdmissionCandidateStateConflictException.class);
        verifyNoInteractions(outboxEventService);
    }

    @Test void nonAdministratorCannotManageProvisioning() {
        assertThatThrownBy(() -> service.manageProvisioning(7L, true, new CurrentUser(2L, "STUDENT"), null, null))
                .isInstanceOf(AdmissionCandidateAccessDeniedException.class);
        verifyNoInteractions(outboxEventRepository, admissionCandidateRepository);
    }
}
