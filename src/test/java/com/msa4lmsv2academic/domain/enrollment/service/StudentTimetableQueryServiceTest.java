package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableEntryQueryResult;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.StudentTimetableScheduleQueryResult;
import com.msa4lmsv2academic.domain.enrollment.request.StudentTimetableSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class StudentTimetableQueryServiceTest {

    @Test
    void returnsOwnedActiveTimetableAndTotalCredits() {
        StudentTimetableQueryRepository repository = mock(StudentTimetableQueryRepository.class);
        when(repository.existsStudentByUserId(93101L)).thenReturn(true);
        when(repository.findActiveTimetable(93101L, (short) 2026, SemesterTerm.FIRST))
                .thenReturn(List.of(queryResult()));
        StudentTimetableQueryService service = new StudentTimetableQueryService(repository);

        var response = service.getMyTimetable(
                new StudentTimetableSearchRequestDTO((short) 2026, SemesterTerm.FIRST),
                new CurrentUser(93101L, "STUDENT")
        );

        assertThat(response.academicYear()).isEqualTo((short) 2026);
        assertThat(response.term()).isEqualTo(SemesterTerm.FIRST);
        assertThat(response.totalCredits()).isEqualTo(3);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().schedules()).hasSize(2);
        verify(repository).findActiveTimetable(93101L, (short) 2026, SemesterTerm.FIRST);
    }

    @Test
    void returnsEmptyTimetableWhenStudentHasNoActiveEnrollment() {
        StudentTimetableQueryRepository repository = mock(StudentTimetableQueryRepository.class);
        when(repository.existsStudentByUserId(93101L)).thenReturn(true);
        when(repository.findActiveTimetable(93101L, (short) 2026, SemesterTerm.SECOND))
                .thenReturn(List.of());
        StudentTimetableQueryService service = new StudentTimetableQueryService(repository);

        var response = service.getMyTimetable(
                new StudentTimetableSearchRequestDTO((short) 2026, SemesterTerm.SECOND),
                new CurrentUser(93101L, "STUDENT")
        );

        assertThat(response.totalCredits()).isZero();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void rejectsNonStudentUser() {
        StudentTimetableQueryRepository repository = mock(StudentTimetableQueryRepository.class);
        StudentTimetableQueryService service = new StudentTimetableQueryService(repository);

        assertThatThrownBy(() -> service.getMyTimetable(
                new StudentTimetableSearchRequestDTO((short) 2026, SemesterTerm.FIRST),
                new CurrentUser(93102L, "PROFESSOR")
        )).isInstanceOf(StudentEnrollmentAccessDeniedException.class);
    }

    @Test
    void rejectsStudentAccountWithoutAcademicProfile() {
        StudentTimetableQueryRepository repository = mock(StudentTimetableQueryRepository.class);
        when(repository.existsStudentByUserId(93101L)).thenReturn(false);
        StudentTimetableQueryService service = new StudentTimetableQueryService(repository);

        assertThatThrownBy(() -> service.getMyTimetable(
                new StudentTimetableSearchRequestDTO((short) 2026, SemesterTerm.FIRST),
                new CurrentUser(93101L, "STUDENT")
        )).isInstanceOf(StudentNotFoundException.class);
    }

    private StudentTimetableEntryQueryResult queryResult() {
        return new StudentTimetableEntryQueryResult(
                93101L,
                93101L,
                93101L,
                "TIME-01",
                "분산시스템",
                (byte) 3,
                CompletionType.MAJOR_REQUIRED,
                "시간표교수",
                "01",
                "공학관 401호",
                List.of(
                        new StudentTimetableScheduleQueryResult(
                                93101L, LectureDayOfWeek.MON, (byte) 1, (byte) 2),
                        new StudentTimetableScheduleQueryResult(
                                93101L, LectureDayOfWeek.WED, (byte) 3, (byte) 4)
                )
        );
    }
}
