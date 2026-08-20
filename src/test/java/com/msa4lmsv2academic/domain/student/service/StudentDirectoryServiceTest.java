package com.msa4lmsv2academic.domain.student.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.domain.student.repository.StudentSearchCondition;
import com.msa4lmsv2academic.domain.student.repository.StudentSearchResult;
import com.msa4lmsv2academic.domain.student.request.StudentSearchRequestDTO;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.error.StudentDirectoryAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentDirectoryServiceTest {

    @Mock
    private StudentQueryRepository studentQueryRepository;

    @InjectMocks
    private StudentDirectoryService studentDirectoryService;

    @Test
    void professorSearchUsesResolvedScopeAndClampedPageCondition() {
        ProfessorStudentScope scope = new ProfessorStudentScope(31L, 41L);
        when(studentQueryRepository.findProfessorScopeByUserId(21L)).thenReturn(Optional.of(scope));
        when(studentQueryRepository.search(any())).thenReturn(new StudentSearchResult(List.of(), 0));

        PageRes<?> response = studentDirectoryService.searchStudents(
                request(2, 500, "  김학생  ", 41L, (byte) 2, (short) 2025,
                        AcademicStatus.ON_LEAVE, "gradeLevel", "desc"),
                new CurrentUser(21L, "PROFESSOR")
        );

        ArgumentCaptor<StudentSearchCondition> captor = ArgumentCaptor.forClass(StudentSearchCondition.class);
        verify(studentQueryRepository).search(captor.capture());
        StudentSearchCondition condition = captor.getValue();
        assertThat(condition.offset()).isEqualTo(100);
        assertThat(condition.limit()).isEqualTo(100);
        assertThat(condition.keyword()).isEqualTo("김학생");
        assertThat(condition.professorScope()).isEqualTo(scope);
        assertThat(condition.sortBy()).isEqualTo("gradeLevel");
        assertThat(condition.descending()).isTrue();
        assertThat(response.items()).isEmpty();
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(100);
    }

    @Test
    void adminSearchHasNoProfessorScopeAndMayFilterTerminalStatus() {
        when(studentQueryRepository.search(any())).thenReturn(new StudentSearchResult(List.of(), 0));

        studentDirectoryService.searchStudents(
                request(null, null, null, null, null, null,
                        AcademicStatus.WITHDRAWN, null, null),
                new CurrentUser(1L, "ADMIN")
        );

        ArgumentCaptor<StudentSearchCondition> captor = ArgumentCaptor.forClass(StudentSearchCondition.class);
        verify(studentQueryRepository).search(captor.capture());
        assertThat(captor.getValue().professorScope()).isNull();
        assertThat(captor.getValue().academicStatus()).isEqualTo(AcademicStatus.WITHDRAWN);
        assertThat(captor.getValue().sortBy()).isEqualTo("name");
    }

    @Test
    void professorCannotRequestTerminalAcademicStatus() {
        when(studentQueryRepository.findProfessorScopeByUserId(21L))
                .thenReturn(Optional.of(new ProfessorStudentScope(31L, 41L)));

        assertThatThrownBy(() -> studentDirectoryService.searchStudents(
                request(null, null, null, null, null, null,
                        AcademicStatus.GRADUATED, null, null),
                new CurrentUser(21L, "PROFESSOR")
        )).isInstanceOf(StudentDirectoryAccessDeniedException.class);

        verify(studentQueryRepository, never()).search(any());
    }

    @Test
    void studentCannotUseDirectoryEvenWhenServiceIsCalledDirectly() {
        assertThatThrownBy(() -> studentDirectoryService.searchStudents(
                request(null, null, null, null, null, null, null, null, null),
                new CurrentUser(51L, "STUDENT")
        )).isInstanceOf(StudentDirectoryAccessDeniedException.class);
    }

    @Test
    void professorWithoutAcademicProfessorRecordGetsNotFound() {
        when(studentQueryRepository.findProfessorScopeByUserId(21L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentDirectoryService.searchStudents(
                request(null, null, null, null, null, null, null, null, null),
                new CurrentUser(21L, "PROFESSOR")
        )).isInstanceOf(ProfessorNotFoundException.class);
    }

    private StudentSearchRequestDTO request(
            Integer page,
            Integer size,
            String keyword,
            Long departmentId,
            Byte gradeLevel,
            Short admissionYear,
            AcademicStatus academicStatus,
            String sortBy,
            String sortDirection
    ) {
        return new StudentSearchRequestDTO(
                page, size, keyword, departmentId, gradeLevel, admissionYear,
                academicStatus, sortBy, sortDirection
        );
    }
}
