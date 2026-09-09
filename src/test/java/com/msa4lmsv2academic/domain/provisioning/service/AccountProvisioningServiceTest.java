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

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private OutboxEventService outboxEventService;
    @InjectMocks private AccountProvisioningService service;

    @Test
    void generatedStudentNumberIsStoredBeforeReturningItToAuth() {
        Department department = Department.create("001", null, "컴퓨터공학과", true);
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
        assertThat(response.loginId()).isEqualTo("26001001");
        assertThat(captor.getValue().getStudentNumber()).isEqualTo(response.loginId());
    }
}
