package com.msa4lmsv2academic.domain.admission.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.msa4lmsv2academic.domain.admission.entity.*;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.*;
import com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
class AdmissionTuitionServiceTest {
    AdmissionCandidate candidate() {
        var d=Department.create("01",null,"학과",true);ReflectionTestUtils.setField(d,"id",2L);
        var u=User.provision(1L,"관리자","admin@example.com",null,null,UserRole.ADMIN);
        var c=AdmissionCandidate.create("학생",LocalDate.of(2008,1,1),"student@example.com",null,null,d,(short)2027,u);
        ReflectionTestUtils.setField(c,"id",7L);c.assignAdvisor(3L);return c;
    }
    @Test void unpaidCandidateCannotCreateStudentOrComplete() {
        var c=candidate();assertThat(c.getStatus()).isEqualTo(AdmissionCandidateStatus.PENDING);
        assertThatThrownBy(()->c.markProvisioned(mock(Student.class))).isInstanceOf(AdmissionCandidateStateConflictException.class);
        assertThatThrownBy(()->c.completeRegistration(10L)).isInstanceOf(AdmissionCandidateStateConflictException.class);
        assertThat(AdmissionCandidateStatus.values()).containsExactly(AdmissionCandidateStatus.PENDING,AdmissionCandidateStatus.COMPLETED,AdmissionCandidateStatus.CANCELLED);
    }
    @Test void paidNotificationIsIdempotentAndCompletionWaitsForAuth() {
        var c=candidate();var repo=mock(AdmissionCandidateRepository.class);var outbox=mock(OutboxEventService.class);
        when(repo.findByIdForUpdate(7L)).thenReturn(Optional.of(c));var service=new AdmissionTuitionService(repo,outbox);
        service.bind(7L,100L);service.paid(7L,100L);service.paid(7L,100L);
        verify(outbox,times(1)).record(eq("ADMISSION_CANDIDATE"),eq(7L),eq("AdmissionCandidateRegistered"),anyMap(),eq(1L));
        assertThat(c.getStatus()).isEqualTo(AdmissionCandidateStatus.PENDING);assertThat(c.isEditable()).isFalse();
        var student=mock(Student.class);when(student.getId()).thenReturn(10L);when(student.getUser()).thenReturn(User.provision(20L,"학생","student@example.com",null,null,UserRole.STUDENT));
        c.markProvisioned(student);assertThat(c.getStatus()).isEqualTo(AdmissionCandidateStatus.PENDING);
        service.activated(7L,20L);service.activated(7L,20L);assertThat(c.getStatus()).isEqualTo(AdmissionCandidateStatus.COMPLETED);
        assertThatThrownBy(()->service.activated(7L,21L)).isInstanceOf(AdmissionCandidateStateConflictException.class);
    }
    @Test void cancellationAndPaymentCannotBothWin() {
        var cancelled=candidate();cancelled.bindTuitionBill(100L);cancelled.cancelProvisioning(cancelled.getCreatedBy());
        assertThatThrownBy(()->cancelled.confirmTuitionPaid(100L)).isInstanceOf(AdmissionCandidateStateConflictException.class);
        var paid=candidate();paid.bindTuitionBill(200L);paid.confirmTuitionPaid(200L);
        assertThatThrownBy(()->paid.cancelProvisioning(paid.getCreatedBy())).isInstanceOf(AdmissionCandidateStateConflictException.class);
        assertThatThrownBy(()->paid.confirmTuitionPaid(201L)).isInstanceOf(AdmissionCandidateStateConflictException.class);
    }
}
