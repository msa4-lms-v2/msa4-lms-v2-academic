package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingParticipantQueryRepository;
import com.msa4lmsv2academic.domain.counseling.repository.CounselorAvailabilityRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselorAvailabilityReplaceRequestDTO;
import com.msa4lmsv2academic.domain.counseling.request.CounselorAvailabilitySlotRequestDTO;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CounselorAvailabilityServiceTest {

    @Mock
    private CounselorAvailabilityRepository availabilityRepository;

    @Mock
    private CounselingParticipantQueryRepository participantQueryRepository;

    private CounselorAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new CounselorAvailabilityService(availabilityRepository, participantQueryRepository);
    }

    @Test
    void professorReplacesOwnAvailabilitySlots() {
        CurrentUser currentUser = new CurrentUser(11L, "PROFESSOR");
        Professor professor = professor(31L, 11L, "김교수");
        CounselorAvailabilitySlotRequestDTO slot = new CounselorAvailabilitySlotRequestDTO(
                DayOfWeek.MONDAY, "09:00", "12:00", LocalDate.of(2026, 9, 1), null
        );
        when(participantQueryRepository.findProfessorByUserIdForUpdate(11L)).thenReturn(Optional.of(professor));
        when(availabilityRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.replaceAvailabilities(
                new CounselorAvailabilityReplaceRequestDTO(List.of(slot)), currentUser
        );

        verify(availabilityRepository).deleteByProfessorId(31L);
        assertThat(result).singleElement().satisfies(saved -> {
            assertThat(saved.professorId()).isEqualTo(31L);
            assertThat(saved.startTime()).isEqualTo("09:00");
        });
    }

    @Test
    void overlappingSlotsAreRejected() {
        LocalDate validFrom = LocalDate.of(2026, 9, 1);
        var request = new CounselorAvailabilityReplaceRequestDTO(List.of(
                new CounselorAvailabilitySlotRequestDTO(
                        DayOfWeek.MONDAY, "09:00", "11:00", validFrom, null
                ),
                new CounselorAvailabilitySlotRequestDTO(
                        DayOfWeek.MONDAY, "10:30", "12:00", validFrom, null
                )
        ));

        assertThatThrownBy(() -> service.replaceAvailabilities(request, new CurrentUser(11L, "PROFESSOR")))
                .isInstanceOf(InvalidCounselingRequestException.class);
    }

    private Professor professor(Long professorId, Long userId, String name) {
        Professor professor = mock(Professor.class);
        User user = mock(User.class);
        lenient().when(professor.getId()).thenReturn(professorId);
        lenient().when(professor.getUser()).thenReturn(user);
        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getName()).thenReturn(name);
        return professor;
    }
}
