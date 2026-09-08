package com.msa4lmsv2academic.domain.grade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.grade.repository.StudentGradeQueryRepository;
import com.msa4lmsv2academic.domain.grade.repository.StudentGradeQueryResult;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSortBy;
import com.msa4lmsv2academic.domain.grade.request.StudentGradeSortDirection;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.error.StudentGradeAccessDeniedException;
import com.msa4lmsv2academic.global.error.StudentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentGradeQueryServiceTest {

    private StudentGradeQueryRepository queryRepository;
    private StudentGradeQueryService service;

    @BeforeEach
    void setUp() {
        queryRepository = mock(StudentGradeQueryRepository.class);
        service = new StudentGradeQueryService(queryRepository);
    }

    @Test
    void returnsOnlyOwnOpenedGradesAndCalculatesRetakeAwareSummaries() {
        CurrentUser student = new CurrentUser(10L, "STUDENT");
        when(queryRepository.existsStudentByUserId(10L)).thenReturn(true);
        when(queryRepository.findDisclosableGradesByStudentUserId(10L)).thenReturn(List.of(
                grade(103L, 2L, (short) 2026, SemesterTerm.FIRST, "CSE200", "운영체제", "B+"),
                grade(102L, 1L, (short) 2026, SemesterTerm.FIRST, "CSE100", "자료구조", "A"),
                grade(101L, 1L, (short) 2025, SemesterTerm.SECOND, "CSE100", "자료구조", "C")
        ));

        var response = service.getMyGrades(
                new StudentGradeSearchRequestDTO(
                        (short) 2025,
                        SemesterTerm.SECOND,
                        "자료",
                        StudentGradeSortBy.ACADEMIC_YEAR,
                        StudentGradeSortDirection.DESC
                ),
                student
        );

        assertThat(response.totalCredits()).isEqualTo(6);
        assertThat(response.totalGpa()).isEqualByComparingTo("3.75");
        assertThat(response.queryCredits()).isZero();
        assertThat(response.queryGpa()).isEqualByComparingTo("0.00");
        assertThat(response.grades()).hasSize(1);
        assertThat(response.grades().getFirst().enrollmentId()).isEqualTo(101L);
        assertThat(response.grades().getFirst().reflectedInGpa()).isFalse();
    }

    @Test
    void returnsZeroSummaryAndEmptyListWhenThereAreNoOpenedGrades() {
        CurrentUser student = new CurrentUser(10L, "STUDENT");
        when(queryRepository.existsStudentByUserId(10L)).thenReturn(true);
        when(queryRepository.findDisclosableGradesByStudentUserId(10L)).thenReturn(List.of());

        var response = service.getMyGrades(null, student);

        assertThat(response.totalGpa()).isEqualByComparingTo("0.00");
        assertThat(response.totalCredits()).isZero();
        assertThat(response.queryGpa()).isEqualByComparingTo("0.00");
        assertThat(response.queryCredits()).isZero();
        assertThat(response.grades()).isEmpty();
    }

    @Test
    void sortsGradesByRequestedGradePointDirection() {
        CurrentUser student = new CurrentUser(10L, "STUDENT");
        when(queryRepository.existsStudentByUserId(10L)).thenReturn(true);
        when(queryRepository.findDisclosableGradesByStudentUserId(10L)).thenReturn(List.of(
                grade(103L, 3L, (short) 2026, SemesterTerm.FIRST, "CSE300", "네트워크", "B+"),
                grade(102L, 2L, (short) 2026, SemesterTerm.FIRST, "CSE200", "운영체제", "A"),
                grade(101L, 1L, (short) 2025, SemesterTerm.SECOND, "CSE100", "자료구조", "C")
        ));

        var response = service.getMyGrades(
                new StudentGradeSearchRequestDTO(
                        null,
                        null,
                        null,
                        StudentGradeSortBy.GRADE,
                        StudentGradeSortDirection.ASC
                ),
                student
        );

        assertThat(response.grades()).extracting(item -> item.letterGrade())
                .containsExactly("C", "B+", "A");
    }

    @Test
    void rejectsNonStudentActor() {
        assertThatThrownBy(() -> service.getMyGrades(null, new CurrentUser(20L, "PROFESSOR")))
                .isInstanceOf(StudentGradeAccessDeniedException.class);
    }

    @Test
    void rejectsStudentWithoutSynchronizedAcademicProfile() {
        when(queryRepository.existsStudentByUserId(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getMyGrades(null, new CurrentUser(99L, "STUDENT")))
                .isInstanceOf(StudentNotFoundException.class);
    }

    private StudentGradeQueryResult grade(
            Long enrollmentId,
            Long courseId,
            short academicYear,
            SemesterTerm term,
            String courseCode,
            String courseName,
            String letterGrade
    ) {
        return new StudentGradeQueryResult(
                enrollmentId,
                courseId,
                enrollmentId + 1000,
                academicYear,
                term,
                courseCode,
                courseName,
                (byte) 3,
                new BigDecimal("90.00"),
                letterGrade
        );
    }
}
