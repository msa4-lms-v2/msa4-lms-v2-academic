package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentEnrollmentQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentEnrollmentQueryResult;
import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentEnrollmentQueryServiceTest {

    @Test
    void returnsActiveEnrollmentsForAuthenticatedStudent() {
        StudentEnrollmentQueryRepository repository = mock(StudentEnrollmentQueryRepository.class);
        StudentEnrollmentQueryResult queryResult = queryResult();
        when(repository.existsStudentByUserId(2001L)).thenReturn(true);
        when(repository.findActiveEnrollmentsByStudentUserId(2001L, (short) 2026, SemesterTerm.FIRST))
                .thenReturn(List.of(queryResult));
        StudentEnrollmentQueryService service = new StudentEnrollmentQueryService(repository);

        List<StudentEnrollmentResponseDTO> response = service.getMyEnrollments(
                new StudentEnrollmentSearchRequestDTO((short) 2026, SemesterTerm.FIRST),
                new CurrentUser(2001L, "STUDENT")
        );

        assertThat(response).containsExactly(StudentEnrollmentResponseDTO.from(queryResult));
        verify(repository).findActiveEnrollmentsByStudentUserId(2001L, (short) 2026, SemesterTerm.FIRST);
    }

    @Test
    void returnsEmptyListWhenStudentHasNoActiveEnrollments() {
        StudentEnrollmentQueryRepository repository = mock(StudentEnrollmentQueryRepository.class);
        when(repository.existsStudentByUserId(2001L)).thenReturn(true);
        when(repository.findActiveEnrollmentsByStudentUserId(2001L, null, null)).thenReturn(List.of());
        StudentEnrollmentQueryService service = new StudentEnrollmentQueryService(repository);

        List<StudentEnrollmentResponseDTO> response = service.getMyEnrollments(
                new StudentEnrollmentSearchRequestDTO(null, null),
                new CurrentUser(2001L, "STUDENT")
        );

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsNonStudentUser() {
        StudentEnrollmentQueryRepository repository = mock(StudentEnrollmentQueryRepository.class);
        StudentEnrollmentQueryService service = new StudentEnrollmentQueryService(repository);

        assertThatThrownBy(() -> service.getMyEnrollments(
                new StudentEnrollmentSearchRequestDTO(null, null),
                new CurrentUser(3001L, "PROFESSOR")
        )).isInstanceOf(StudentEnrollmentAccessDeniedException.class);
    }

    @Test
    void rejectsStudentAccountWithoutAcademicStudentProfile() {
        StudentEnrollmentQueryRepository repository = mock(StudentEnrollmentQueryRepository.class);
        when(repository.existsStudentByUserId(2001L)).thenReturn(false);
        StudentEnrollmentQueryService service = new StudentEnrollmentQueryService(repository);

        assertThatThrownBy(() -> service.getMyEnrollments(
                new StudentEnrollmentSearchRequestDTO(null, null),
                new CurrentUser(2001L, "STUDENT")
        )).isInstanceOf(StudentNotFoundException.class);
    }

    private StudentEnrollmentQueryResult queryResult() {
        return new StudentEnrollmentQueryResult(
                501L,
                EnrollmentStatus.ACTIVE,
                LocalDateTime.of(2026, 2, 10, 10, 30),
                101L,
                31L,
                "CSE301",
                "운영체제",
                (byte) 3,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED,
                "컴퓨터공학과",
                "홍길동",
                (short) 2026,
                SemesterTerm.FIRST,
                "01",
                "공학관 301호",
                40,
                LectureStatus.OPEN
        );
    }
}
