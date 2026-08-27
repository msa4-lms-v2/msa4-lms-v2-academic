package com.msa4lmsv2academic.domain.academicstatus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.academicstatus.request.AcademicStatusHistorySearchRequestDTO;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistoryQueryRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.AcademicStatusHistorySearchCondition;
import com.msa4lmsv2academic.global.error.AcademicStatusHistoryAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AcademicStatusHistoryServiceTest {

    @Mock private AcademicStatusHistoryQueryRepository historyQueryRepository;
    @Mock private StudentQueryRepository studentQueryRepository;
    @InjectMocks private AcademicStatusHistoryService service;
    private final PageRequest pageable = PageRequest.of(0, 20);
    private final AcademicStatusHistorySearchRequestDTO request = new AcademicStatusHistorySearchRequestDTO(
            null, null, "  김학생  ", 50L, null, null, null, null, null, null, null);

    @Test
    void rejectsUnauthenticatedInvalidIdentityAndUnsupportedRoleBeforeQuery() {
        for (CurrentUser user : Arrays.asList(null, new CurrentUser(null, "ADMIN"), new CurrentUser(-1L, "ADMIN"),
                new CurrentUser(1L, "SYSTEM"), new CurrentUser(1L, null))) {
            assertThatThrownBy(() -> service.search(request, pageable, user))
                    .isInstanceOf(AcademicStatusHistoryAccessDeniedException.class);
        }
        verifyNoInteractions(historyQueryRepository, studentQueryRepository);
    }

    @Test
    void studentScopeUsesAuthenticatedUserRatherThanRequestedStudent() {
        when(historyQueryRepository.search(any(), any())).thenReturn(Page.empty(pageable));
        service.search(request, pageable, new CurrentUser(7L, "STUDENT"));
        var captor = ArgumentCaptor.forClass(AcademicStatusHistorySearchCondition.class);
        verify(historyQueryRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerUserId()).isEqualTo(7L);
        assertThat(captor.getValue().studentId()).isEqualTo(50L);
        assertThat(captor.getValue().keyword()).isEqualTo("김학생");
        assertThat(captor.getValue().professorScope()).isNull();
        verifyNoInteractions(studentQueryRepository);
    }

    @Test
    void adminHasNoOwnerOrProfessorRestriction() {
        when(historyQueryRepository.search(any(), any())).thenReturn(Page.empty(pageable));
        service.search(request, pageable, new CurrentUser(1L, "ADMIN"));
        var captor = ArgumentCaptor.forClass(AcademicStatusHistorySearchCondition.class);
        verify(historyQueryRepository).search(captor.capture(), any());
        assertThat(captor.getValue().ownerUserId()).isNull();
        assertThat(captor.getValue().professorScope()).isNull();
        verifyNoInteractions(studentQueryRepository);
    }

    @Test
    void professorScopeReusesExistingIdentityLookup() {
        var scope = new ProfessorStudentScope(20L, 30L);
        when(studentQueryRepository.findProfessorScopeByUserId(1L)).thenReturn(Optional.of(scope));
        when(historyQueryRepository.search(any(), any())).thenReturn(Page.empty(pageable));
        service.search(request, pageable, new CurrentUser(1L, "PROFESSOR"));
        var captor = ArgumentCaptor.forClass(AcademicStatusHistorySearchCondition.class);
        verify(historyQueryRepository).search(captor.capture(), any());
        assertThat(captor.getValue().professorScope()).isEqualTo(scope);
        assertThat(captor.getValue().ownerUserId()).isNull();
    }

    @Test
    void professorWithoutProfileIsNotTreatedAsAdmin() {
        when(studentQueryRepository.findProfessorScopeByUserId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.search(request, pageable, new CurrentUser(1L, "PROFESSOR")))
                .isInstanceOf(ProfessorNotFoundException.class);
        verifyNoInteractions(historyQueryRepository);
    }
}
