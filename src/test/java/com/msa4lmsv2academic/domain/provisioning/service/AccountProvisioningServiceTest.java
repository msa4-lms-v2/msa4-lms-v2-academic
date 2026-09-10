package com.msa4lmsv2academic.domain.provisioning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountProvisioningServiceTest {

    @Test
    void cancelledCandidateCannotCreateAStudentEvenWhenARequestArrivesLate() {
        var candidate = com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate.create(
                "학생", java.time.LocalDate.of(2008, 1, 1), "s@example.com", null, null, null, (short) 2026, null);
        candidate.cancelProvisioning(null);
        when(admissionCandidateRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(candidate));
        var request = new StudentProvisioningRequestDTO(23L, "학생", "s@example.com", null, null,
                1L, (short) 2026, 7L, 2L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.provisionStudent(request))
                .isInstanceOf(com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException.class);
        org.mockito.Mockito.verifyNoInteractions(userRepository, departmentRepository, outboxEventService);
        verify(studentRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private OutboxEventService outboxEventService;
    @Mock private com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository admissionCandidateRepository;
    @InjectMocks private AccountProvisioningService service;

    @Test
    void numberFormatPadsDepartmentAndIdAndRejectsOverflow() {
        assertThat(AcademicNumberGenerator.generate((short) 2026, 1L, 23L)).isEqualTo("26010023");
        assertThat(AcademicNumberGenerator.generate((short) 2026, 99L, 9999L)).isEqualTo("26999999");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AcademicNumberGenerator.generate((short) 2026, 100L, 1L))
                .isInstanceOf(com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AcademicNumberGenerator.generate((short) 2026, 1L, 10000L))
                .isInstanceOf(com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException.class);
    }

    @Test
    void professorNumberKeepsPrefixAndRetryReturnsStoredNumber() {
        Department department = Department.create("001", null, "컴퓨터공학과", true);
        ReflectionTestUtils.setField(department, "id", 10L);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(professorRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            var professor = (com.msa4lmsv2academic.domain.professor.entity.Professor) invocation.getArgument(0);
            ReflectionTestUtils.setField(professor, "id", 23L);
            return professor;
        });
        var request = new com.msa4lmsv2academic.domain.provisioning.request.ProfessorProvisioningRequestDTO(
                1001L, "김교수", "professor@example.com", null, null, 10L, (short) 2026);
        var response = service.provisionProfessor(request);
        assertThat(response.loginId()).isEqualTo("p26100023");
        ArgumentCaptor<com.msa4lmsv2academic.domain.professor.entity.Professor> captor = ArgumentCaptor.forClass(com.msa4lmsv2academic.domain.professor.entity.Professor.class);
        verify(professorRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getProfessorNumber()).isEqualTo(response.loginId());
        when(professorRepository.findByUserId(1001L)).thenReturn(Optional.of(captor.getValue()));
        assertThat(service.provisionProfessor(request).loginId()).isEqualTo(response.loginId());
        verify(professorRepository, org.mockito.Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void admissionProvisioningLinksStudentAndIsIdempotent() {
        Department department = Department.create("001", null, "컴퓨터공학과", true);
        ReflectionTestUtils.setField(department, "id", 10L);
        var candidate = com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate.create(
                "김학생", java.time.LocalDate.of(2008, 3, 15), "student@example.com", null, null, department, (short) 2026, null);
        var advisorUser = com.msa4lmsv2academic.domain.user.entity.User.provision(
                2001L, "김교수", "advisor@example.com", null, null,
                com.msa4lmsv2academic.domain.user.entity.UserRole.PROFESSOR);
        var advisor = com.msa4lmsv2academic.domain.professor.entity.Professor.create(advisorUser, (short) 2020, department);
        ReflectionTestUtils.setField(advisor, "id", 15L);
        when(admissionCandidateRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(candidate));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(professorRepository.findById(15L)).thenReturn(Optional.of(advisor));
        when(studentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", 23L);
            return student;
        });
        var request = new StudentProvisioningRequestDTO(1001L, "김학생", "student@example.com", null, null, 10L, (short) 2026, 7L, 15L);
        assertThat(service.provisionStudent(request).loginId()).isEqualTo("26100023");
        assertThat(candidate.getStatus()).isEqualTo(com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus.PROVISIONED);
        assertThat(candidate.getStudent().getStudentNumber()).isEqualTo("26100023");
        assertThat(candidate.getStudent().getAdvisor()).isEqualTo(advisor);
        when(studentRepository.findByUserId(1001L)).thenReturn(Optional.of(candidate.getStudent()));
        assertThat(service.provisionStudent(request).loginId()).isEqualTo("26100023");
        verify(studentRepository, org.mockito.Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void generatedStudentNumberIsStoredBeforeReturningItToAuth() {
        Department department = Department.create("001", null, "컴퓨터공학과", true);
        ReflectionTestUtils.setField(department, "id", 10L);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            ReflectionTestUtils.setField(student, "id", 1L);
            return student;
        });

        var response = service.provisionStudent(new StudentProvisioningRequestDTO(
                1001L, "김학생", "student@example.com", "010-1234-5678", "서울", 10L, (short) 2026));

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).saveAndFlush(captor.capture());
        verify(studentRepository).flush();
        assertThat(response.loginId()).isEqualTo("26100001");
        assertThat(captor.getValue().getStudentNumber()).isEqualTo(response.loginId());
    }
}
