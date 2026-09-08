package com.msa4lmsv2academic.domain.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationQueryRepository;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationQueryResult;
import com.msa4lmsv2academic.domain.evaluation.repository.ProfessorLectureEvaluationSearchResult;
import com.msa4lmsv2academic.domain.evaluation.request.ProfessorLectureEvaluationSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.ProfessorLectureAccessDeniedException;
import com.msa4lmsv2academic.global.error.ProfessorNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfessorLectureEvaluationQueryServiceTest {

    private static final Long PROFESSOR_USER_ID = 20L;
    private static final Long LECTURE_ID = 30L;

    private ProfessorLectureEvaluationQueryRepository queryRepository;
    private ProfessorLectureEvaluationQueryService service;

    @BeforeEach
    void setUp() {
        queryRepository = mock(ProfessorLectureEvaluationQueryRepository.class);
        service = new ProfessorLectureEvaluationQueryService(queryRepository);
        when(queryRepository.existsProfessorByUserId(PROFESSOR_USER_ID)).thenReturn(true);
    }

    @Test
    void returnsAnonymousAveragesCommentsAndResponseRateForOwnedLecture() {
        var lecture = new ProfessorLectureEvaluationQueryResult(
                LECTURE_ID,
                "CSE301",
                "소프트웨어공학",
                "01",
                (short) 2026,
                SemesterTerm.FIRST,
                4L
        );
        when(queryRepository.searchLecturesByProfessorUserId(
                PROFESSOR_USER_ID, LECTURE_ID, (short) 2026, SemesterTerm.FIRST, true, 0L, 20
        )).thenReturn(new ProfessorLectureEvaluationSearchResult(List.of(lecture), 1L));
        LectureEvaluation firstEvaluation = evaluation(
                Map.of("CONTENT_QUALITY", 5, "DELIVERY_CLARITY", 3),
                "실습이 좋았습니다."
        );
        LectureEvaluation secondEvaluation = evaluation(
                Map.of("CONTENT_QUALITY", 3, "DELIVERY_CLARITY", 5),
                "   "
        );
        when(queryRepository.findEvaluationResponses(PROFESSOR_USER_ID, List.of(LECTURE_ID)))
                .thenReturn(List.of(firstEvaluation, secondEvaluation));

        var result = service.getMyResults(
                new ProfessorLectureEvaluationSearchRequestDTO(
                        1, 20, LECTURE_ID, (short) 2026, SemesterTerm.FIRST, true
                ),
                professor()
        );

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
        var item = result.items().getFirst();
        assertThat(item.responseCount()).isEqualTo(2L);
        assertThat(item.responseRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(item.overallAverage()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(item.questionAverages()).containsOnly(
                org.assertj.core.api.Assertions.entry("CONTENT_QUALITY", new BigDecimal("4.00")),
                org.assertj.core.api.Assertions.entry("DELIVERY_CLARITY", new BigDecimal("4.00"))
        );
        assertThat(item.comments()).containsExactly("실습이 좋았습니다.");
        assertThat(item.hasResponses()).isTrue();
    }

    @Test
    void returnsClearEmptyStatisticsWhenOwnedLectureHasNoResponses() {
        var lecture = new ProfessorLectureEvaluationQueryResult(
                LECTURE_ID, "CSE301", "소프트웨어공학", "01",
                (short) 2026, SemesterTerm.FIRST, 0L
        );
        when(queryRepository.searchLecturesByProfessorUserId(
                PROFESSOR_USER_ID, null, null, null, null, 0L, 20
        )).thenReturn(new ProfessorLectureEvaluationSearchResult(List.of(lecture), 1L));
        when(queryRepository.findEvaluationResponses(PROFESSOR_USER_ID, List.of(LECTURE_ID)))
                .thenReturn(List.of());

        var result = service.getMyResults(null, professor());

        var item = result.items().getFirst();
        assertThat(item.responseCount()).isZero();
        assertThat(item.responseRate()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(item.hasResponses()).isFalse();
        assertThat(item.overallAverage()).isNull();
        assertThat(item.questionAverages()).isEmpty();
        assertThat(item.comments()).isEmpty();
    }

    @Test
    void rejectsNonProfessorBeforeQueryingEvaluationData() {
        assertThatThrownBy(() -> service.getMyResults(null, new CurrentUser(10L, "STUDENT")))
                .isInstanceOf(ProfessorLectureAccessDeniedException.class);
        assertThatThrownBy(() -> service.getMyResults(null, null))
                .isInstanceOf(ProfessorLectureAccessDeniedException.class);

        verify(queryRepository, never()).findEvaluationResponses(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void rejectsProfessorWithoutAcademicProfile() {
        when(queryRepository.existsProfessorByUserId(PROFESSOR_USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getMyResults(null, professor()))
                .isInstanceOf(ProfessorNotFoundException.class);
    }

    private LectureEvaluation evaluation(Map<String, Integer> ratings, String comment) {
        Lecture lecture = mock(Lecture.class);
        when(lecture.getId()).thenReturn(LECTURE_ID);
        Enrollment enrollment = mock(Enrollment.class);
        when(enrollment.getLecture()).thenReturn(lecture);
        LectureEvaluation evaluation = mock(LectureEvaluation.class);
        when(evaluation.getEnrollment()).thenReturn(enrollment);
        when(evaluation.getRatings()).thenReturn(ratings);
        when(evaluation.getComment()).thenReturn(comment);
        return evaluation;
    }

    private CurrentUser professor() {
        return new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    }
}
