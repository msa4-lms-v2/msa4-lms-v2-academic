package com.msa4lmsv2academic.domain.professor.controller;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfessorCertificateControllerTest {
    @Test void resolvesProfessorFromAuthenticatedUser() {
        var professors = mock(ProfessorRepository.class);
        var lectures = mock(LectureRepository.class);
        var professor = mock(Professor.class, RETURNS_DEEP_STUBS);
        when(professor.getId()).thenReturn(8L);
        when(professor.getUser().getId()).thenReturn(7L);
        when(professor.getUser().getStatus()).thenReturn(UserStatus.ACTIVE);
        when(professors.findByUserId(7L)).thenReturn(Optional.of(professor));
        when(lectures.findCertificateCareer(eq(8L), any(LocalDate.class))).thenReturn(List.of());
        var response = new ProfessorCertificateController(professors, lectures)
                .getCareer(new CurrentUser(7L, "PROFESSOR"));
        assertEquals(7L, response.data().userId());
        assertEquals(8L, response.data().professor().professorId());
        assertTrue(response.data().lectures().isEmpty());
        verify(lectures).findCertificateCareer(8L, LocalDate.now());
    }

    @Test void missingProfessorCannotReadTeachingHistory() {
        var professors = mock(ProfessorRepository.class);
        var lectures = mock(LectureRepository.class);
        when(professors.findByUserId(7L)).thenReturn(Optional.empty());
        assertThrows(ProfessorNotFoundException.class, () ->
                new ProfessorCertificateController(professors, lectures).getCareer(new CurrentUser(7L, "PROFESSOR")));
        verifyNoInteractions(lectures);
    }
}
