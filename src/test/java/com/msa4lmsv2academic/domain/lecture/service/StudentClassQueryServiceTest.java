package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentClassQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentClassQueryResult;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.request.StudentClassSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.StudentClassResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.StudentClassAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentClassQueryServiceTest {

    @Test
    void returnsActiveClassesForAuthenticatedStudent() {
        StudentClassQueryRepository repository = mock(StudentClassQueryRepository.class);
        StudentClassQueryResult queryResult = queryResult();
        when(repository.existsStudentByUserId(2001L)).thenReturn(true);
        when(repository.findActiveClassesByStudentUserId(2001L, (short) 2026, SemesterTerm.FIRST))
                .thenReturn(List.of(queryResult));
        StudentClassQueryService service = new StudentClassQueryService(repository);

        List<StudentClassResponseDTO> response = service.getMyClasses(
                new StudentClassSearchRequestDTO((short) 2026, SemesterTerm.FIRST),
                new CurrentUser(2001L, "STUDENT")
        );

        assertThat(response).containsExactly(StudentClassResponseDTO.from(queryResult));
        verify(repository).findActiveClassesByStudentUserId(2001L, (short) 2026, SemesterTerm.FIRST);
    }

    @Test
    void returnsEmptyListWhenStudentHasNoActiveClasses() {
        StudentClassQueryRepository repository = mock(StudentClassQueryRepository.class);
        when(repository.existsStudentByUserId(2001L)).thenReturn(true);
        when(repository.findActiveClassesByStudentUserId(2001L, null, null)).thenReturn(List.of());
        StudentClassQueryService service = new StudentClassQueryService(repository);

        List<StudentClassResponseDTO> response = service.getMyClasses(
                new StudentClassSearchRequestDTO(null, null),
                new CurrentUser(2001L, "STUDENT")
        );

        assertThat(response).isEmpty();
    }

    @Test
    void rejectsNonStudentUser() {
        StudentClassQueryRepository repository = mock(StudentClassQueryRepository.class);
        StudentClassQueryService service = new StudentClassQueryService(repository);

        assertThatThrownBy(() -> service.getMyClasses(
                new StudentClassSearchRequestDTO(null, null),
                new CurrentUser(3001L, "PROFESSOR")
        )).isInstanceOf(StudentClassAccessDeniedException.class);
    }

    @Test
    void rejectsStudentAccountWithoutAcademicStudentProfile() {
        StudentClassQueryRepository repository = mock(StudentClassQueryRepository.class);
        when(repository.existsStudentByUserId(2001L)).thenReturn(false);
        StudentClassQueryService service = new StudentClassQueryService(repository);

        assertThatThrownBy(() -> service.getMyClasses(
                new StudentClassSearchRequestDTO(null, null),
                new CurrentUser(2001L, "STUDENT")
        )).isInstanceOf(StudentNotFoundException.class);
    }

    private StudentClassQueryResult queryResult() {
        return new StudentClassQueryResult(
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
