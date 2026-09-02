package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.CourseGradeAttemptQueryResult;
import com.msa4lmsv2academic.domain.enrollment.repository.CoursePrerequisiteEdge;
import com.msa4lmsv2academic.domain.enrollment.repository.CoursePrerequisiteRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleSearchCondition;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleSearchResult;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleQueryResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.InvalidPrerequisiteRetakeRuleRequestException;
import com.msa4lmsv2academic.global.error.PrerequisiteRetakeRuleAccessDeniedException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrerequisiteRetakeRuleServiceTest {

    private static final long STUDENT_ID = 8L;
    private static final long STUDENT_USER_ID = 18L;
    private static final long COURSE_ID = 20L;

    @Test
    void treatsOnlyFHistoryAsFirstEnrollment() {
        Fixture fixture = fixture(List.of(attempt(COURSE_ID, GradeStatus.OPENED, "F", false)));

        PrerequisiteRetakeEvaluationResponseDTO evaluation = fixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation();

        assertThat(evaluation.retakeCondition().status()).isEqualTo(RetakeStatus.FIRST_ENROLLMENT);
        assertThat(evaluation.retakeCondition().satisfied()).isTrue();
        assertThat(evaluation.ruleSatisfied()).isTrue();
        verify(fixture.evaluator()).evaluate(any(Long.class), any(Course.class));
    }

    @Test
    void doesNotEvaluateWhenAdminOnlySearchesCriteria() {
        Fixture fixture = fixture(List.of());

        PrerequisiteRetakeRuleQueryResponseDTO response = fixture.service().search(
                searchRequest(COURSE_ID), new CurrentUser(99L, "ADMIN")
        );

        assertThat(response.criteria().items()).isEmpty();
        assertThat(response.evaluation()).isNull();
        verifyNoInteractions(fixture.evaluator());
    }

    @Test
    void rejectsStudentIdOverrideBeforeEvaluation() {
        Fixture fixture = fixture(List.of());
        PrerequisiteRetakeRuleSearchRequestDTO request = new PrerequisiteRetakeRuleSearchRequestDTO(
                1, 20, null, COURSE_ID, STUDENT_ID + 1, null, "courseCode", "asc"
        );

        assertThatThrownBy(() -> fixture.service().search(request, new CurrentUser(STUDENT_USER_ID, "STUDENT")))
                .isInstanceOf(PrerequisiteRetakeRuleAccessDeniedException.class);
        verifyNoInteractions(fixture.evaluator());
    }

    @Test
    void allowsCPlusButBlocksWhenAnyBOrHigherGradeExists() {
        Fixture allowedFixture = fixture(List.of(
                attempt(COURSE_ID, GradeStatus.OPENED, "C+", false)
        ));
        PrerequisiteRetakeEvaluationResponseDTO allowed = allowedFixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation();

        Fixture blockedFixture = fixture(List.of(
                attempt(COURSE_ID, GradeStatus.OPENED, "C", false),
                attempt(COURSE_ID, GradeStatus.OPENED, "B", false)
        ));
        PrerequisiteRetakeEvaluationResponseDTO blocked = blockedFixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation();

        assertThat(allowed.retakeCondition().status()).isEqualTo(RetakeStatus.RETAKE_ALLOWED);
        assertThat(allowed.retakeCondition().referenceGrade()).isEqualTo("C+");
        assertThat(blocked.retakeCondition().status()).isEqualTo(RetakeStatus.RETAKE_BLOCKED);
        assertThat(blocked.retakeCondition().referenceGrade()).isEqualTo("B");
        assertThat(blocked.ruleSatisfied()).isFalse();
    }

    @Test
    void requiresEveryDirectPrerequisite() {
        Fixture fixture = fixture(List.of());
        Course prerequisiteCourse = course(10L, "CSE2001", "자료구조");
        CoursePrerequisite rule = mock(CoursePrerequisite.class);
        when(rule.getId()).thenReturn(4L);
        when(rule.getPrerequisiteCourse()).thenReturn(prerequisiteCourse);
        when(fixture.queryRepository().findActiveRulesByCourseId(COURSE_ID)).thenReturn(List.of(rule));

        PrerequisiteRetakeEvaluationResponseDTO evaluation = fixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation();

        assertThat(evaluation.prerequisiteSatisfied()).isFalse();
        assertThat(evaluation.prerequisites()).hasSize(1);
        assertThat(evaluation.prerequisites().getFirst().satisfied()).isFalse();
        assertThat(evaluation.ruleSatisfied()).isFalse();
        assertThat(evaluation.reasons()).extracting(reason -> reason.code().name())
                .containsExactly("PREREQUISITE_NOT_COMPLETED");
    }

    @Test
    void rejectsActiveCurrentEnrollmentAndUnopenedHistoricalGrade() {
        Fixture activeFixture = fixture(List.of(
                attempt(COURSE_ID, GradeStatus.DRAFT, null, true)
        ));
        Fixture pendingFixture = fixture(List.of(
                attempt(COURSE_ID, GradeStatus.DRAFT, null, false)
        ));

        assertThat(activeFixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation().retakeCondition().status()).isEqualTo(RetakeStatus.ACTIVE_ENROLLMENT_EXISTS);
        assertThat(pendingFixture.service().search(
                searchRequest(COURSE_ID),
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        ).evaluation().retakeCondition().status()).isEqualTo(RetakeStatus.GRADE_PENDING);
    }

    @Test
    void rejectsIndirectCycleWhenAdminCreatesRule() {
        CoursePrerequisiteRepository repository = mock(CoursePrerequisiteRepository.class);
        PrerequisiteRetakeRuleQueryRepository queryRepository = mock(PrerequisiteRetakeRuleQueryRepository.class);
        Course targetCourse = course(1L, "C1", "과목1");
        Course prerequisiteCourse = course(2L, "C2", "과목2");
        when(queryRepository.findCourseById(1L)).thenReturn(Optional.of(targetCourse));
        when(queryRepository.findCourseById(2L)).thenReturn(Optional.of(prerequisiteCourse));
        when(repository.findByCourseIdAndPrerequisiteCourseId(1L, 2L)).thenReturn(Optional.empty());
        when(queryRepository.findActiveEdges()).thenReturn(List.of(
                new CoursePrerequisiteEdge(2L, 2L, 3L),
                new CoursePrerequisiteEdge(3L, 3L, 1L)
        ));
        PrerequisiteRetakeRuleService service = new PrerequisiteRetakeRuleService(
                repository,
                queryRepository,
                mock(StudentQueryRepository.class),
                mock(AuditLogService.class),
                new PrerequisiteRetakeEvaluator(queryRepository)
        );

        assertThatThrownBy(() -> service.create(
                new PrerequisiteRetakeRuleCreateRequestDTO(1L, 2L, "순환 검증"),
                new CurrentUser(99L, "ADMIN"),
                "request-1",
                "127.0.0.1"
        )).isInstanceOf(InvalidPrerequisiteRetakeRuleRequestException.class);
    }

    private Fixture fixture(List<CourseGradeAttemptQueryResult> attempts) {
        CoursePrerequisiteRepository repository = mock(CoursePrerequisiteRepository.class);
        PrerequisiteRetakeRuleQueryRepository queryRepository = mock(PrerequisiteRetakeRuleQueryRepository.class);
        StudentQueryRepository studentQueryRepository = mock(StudentQueryRepository.class);
        Course targetCourse = course(COURSE_ID, "CSE3001", "운영체제");
        when(queryRepository.findStudentIdByUserId(STUDENT_USER_ID)).thenReturn(Optional.of(STUDENT_ID));
        when(queryRepository.findCourseById(COURSE_ID)).thenReturn(Optional.of(targetCourse));
        when(queryRepository.search(any(PrerequisiteRetakeRuleSearchCondition.class)))
                .thenReturn(new PrerequisiteRetakeRuleSearchResult(List.of(), 0));
        when(queryRepository.findActiveRulesByCourseId(COURSE_ID)).thenReturn(List.of());
        when(queryRepository.findGradeAttempts(any(Long.class), anyList())).thenReturn(attempts);
        PrerequisiteRetakeEvaluator evaluator = spy(new PrerequisiteRetakeEvaluator(queryRepository));
        PrerequisiteRetakeRuleService service = new PrerequisiteRetakeRuleService(
                repository,
                queryRepository,
                studentQueryRepository,
                mock(AuditLogService.class),
                evaluator
        );
        return new Fixture(service, queryRepository, evaluator);
    }

    private PrerequisiteRetakeRuleSearchRequestDTO searchRequest(Long courseId) {
        return new PrerequisiteRetakeRuleSearchRequestDTO(
                1,
                20,
                null,
                courseId,
                null,
                null,
                "courseCode",
                "asc"
        );
    }

    private CourseGradeAttemptQueryResult attempt(
            long courseId,
            GradeStatus gradeStatus,
            String letterGrade,
            boolean currentSemester
    ) {
        return new CourseGradeAttemptQueryResult(
                courseId,
                courseId * 10,
                EnrollmentStatus.ACTIVE,
                gradeStatus,
                letterGrade,
                (short) 2026,
                SemesterTerm.FIRST,
                currentSemester
        );
    }

    private Course course(long id, String code, String name) {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(id);
        when(course.getCode()).thenReturn(code);
        when(course.getName()).thenReturn(name);
        return course;
    }

    private record Fixture(
            PrerequisiteRetakeRuleService service,
            PrerequisiteRetakeRuleQueryRepository queryRepository,
            PrerequisiteRetakeEvaluator evaluator
    ) {
    }
}
