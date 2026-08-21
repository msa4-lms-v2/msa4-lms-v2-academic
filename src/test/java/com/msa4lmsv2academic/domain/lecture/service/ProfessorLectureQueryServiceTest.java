package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureQueryRepository;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureQueryResult;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureScheduleQueryResult;
import com.msa4lmsv2academic.domain.lecture.repository.ProfessorLectureSearchResult;
import com.msa4lmsv2academic.domain.lecture.request.ProfessorLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.ProfessorLectureAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessorLectureQueryServiceTest {

    @Test
    void returnsOnlyAuthenticatedProfessorLecturesWithPageMetadata() {
        ProfessorLectureQueryRepository repository = mock(ProfessorLectureQueryRepository.class);
        ProfessorLectureQueryResult queryResult = queryResult();
        when(repository.existsProfessorByUserId(3001L)).thenReturn(true);
        when(repository.searchByProfessorUserId(
                3001L,
                (short) 2026,
                SemesterTerm.FIRST,
                LectureStatus.OPEN,
                0L,
                20
        )).thenReturn(new ProfessorLectureSearchResult(List.of(queryResult), 1L));
        ProfessorLectureQueryService service = new ProfessorLectureQueryService(repository);

        PageResponseDTO<?> response = service.getMyLectures(
                new ProfessorLectureSearchRequestDTO(
                        1,
                        20,
                        (short) 2026,
                        SemesterTerm.FIRST,
                        LectureStatus.OPEN
                ),
                new CurrentUser(3001L, "PROFESSOR")
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.hasNext()).isFalse();
        verify(repository).searchByProfessorUserId(
                3001L,
                (short) 2026,
                SemesterTerm.FIRST,
                LectureStatus.OPEN,
                0L,
                20
        );
    }

    @Test
    void returnsEmptyPageAndClampsPageSize() {
        ProfessorLectureQueryRepository repository = mock(ProfessorLectureQueryRepository.class);
        when(repository.existsProfessorByUserId(3001L)).thenReturn(true);
        when(repository.searchByProfessorUserId(3001L, null, null, null, 0L, 100))
                .thenReturn(new ProfessorLectureSearchResult(List.of(), 0L));
        ProfessorLectureQueryService service = new ProfessorLectureQueryService(repository);

        PageResponseDTO<?> response = service.getMyLectures(
                new ProfessorLectureSearchRequestDTO(1, 500, null, null, null),
                new CurrentUser(3001L, "PROFESSOR")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void rejectsNonProfessorUser() {
        ProfessorLectureQueryService service = new ProfessorLectureQueryService(
                mock(ProfessorLectureQueryRepository.class)
        );

        assertThatThrownBy(() -> service.getMyLectures(
                new ProfessorLectureSearchRequestDTO(null, null, null, null, null),
                new CurrentUser(2001L, "STUDENT")
        )).isInstanceOf(ProfessorLectureAccessDeniedException.class);
    }

    @Test
    void rejectsProfessorAccountWithoutAcademicProfessorProfile() {
        ProfessorLectureQueryRepository repository = mock(ProfessorLectureQueryRepository.class);
        when(repository.existsProfessorByUserId(3001L)).thenReturn(false);
        ProfessorLectureQueryService service = new ProfessorLectureQueryService(repository);

        assertThatThrownBy(() -> service.getMyLectures(
                new ProfessorLectureSearchRequestDTO(null, null, null, null, null),
                new CurrentUser(3001L, "PROFESSOR")
        )).isInstanceOf(ProfessorNotFoundException.class);
    }

    private ProfessorLectureQueryResult queryResult() {
        return new ProfessorLectureQueryResult(
                101L,
                31L,
                "CSE301",
                "운영체제",
                (byte) 3,
                (byte) 3,
                CompletionType.MAJOR_REQUIRED,
                "컴퓨터공학과",
                12L,
                "홍길동",
                5L,
                (short) 2026,
                SemesterTerm.FIRST,
                "01",
                "공학관 301호",
                40,
                LectureStatus.OPEN,
                30,
                30,
                30,
                10,
                "강의계획서",
                32L,
                List.of(new ProfessorLectureScheduleQueryResult(
                        101L,
                        LectureDayOfWeek.MON,
                        (byte) 1,
                        (byte) 2
                ))
        );
    }
}
