package com.msa4lmsv2academic.domain.grade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.grade.request.GradeFinalizeRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeSaveRequestDTO;
import com.msa4lmsv2academic.domain.grade.request.GradeScoreRequestDTO;
import com.msa4lmsv2academic.global.error.GradeManagementAccessDeniedException;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import com.msa4lmsv2academic.global.error.InvalidGradeManagementRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GradeManagementIntegrationTest extends MySqlIntegrationTest {

    private static final long CLASS_ID = 99601L;
    private static final long ENROLLMENT_ID = 99601L;
    private static final long PROFESSOR_USER_ID = 99601L;

    @Autowired
    private GradeManagementService gradeManagementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99601, 'GRD-COL', '성적테스트대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99601, 'GRD', 99601, '성적테스트학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99601, '성적담당교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99602, '성적대상학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99603, '다른교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99601, 0, 99601, 2020, 99601)");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99602, 0, 99603, 2020, 99601)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (99601, 99602, 99601, 2, 2025, 'ENROLLED', 99601)");
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99601, 2026, 'SECOND', '2026-08-31', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO grade_operation_periods "
                        + "(semester_id, operation_type, start_date, end_date, is_active) "
                        + "VALUES (99601, 'GRADE_ENTRY', ?, ?, 1)",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (99601, 99601, 'GRADE-01', '성적관리테스트', 3, 2, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                + "VALUES (99601, 99601, 99601, 99601, '01', 40, '공학관 301호', 'OPEN', "
                + "30, 30, 20, 20, '성적관리 테스트 강의계획서')");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (99601, 99601, 99601, 'ACTIVE', ?, 'DRAFT')",
                LocalDateTime.of(2026, 8, 5, 9, 0));
    }

    @Test
    void createsPartialDraftAndReplaysSameRequest() {
        GradeSaveRequestDTO request = new GradeSaveRequestDTO(CLASS_ID, List.of(
                new GradeScoreRequestDTO(
                        ENROLLMENT_ID, new BigDecimal("90.00"), null, null, null
                )
        ));
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");

        var first = gradeManagementService.createDraft(
                request, "grade-create-replay", professor, "trace-create", "127.0.0.1"
        );
        var replay = gradeManagementService.createDraft(
                request, "grade-create-replay", professor, "trace-replay", "127.0.0.1"
        );

        assertThat(replay).isEqualTo(first);
        assertThat(first.data().grades().getFirst().midtermScore()).isEqualByComparingTo("90.00");
        assertThat(first.data().grades().getFirst().totalScore()).isNull();
        assertThat(first.data().grades().getFirst().letterGrade()).isNull();
        assertThat(first.data().grades().getFirst().gradeStatus()).isEqualTo(GradeStatus.DRAFT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' "
                        + "AND target_id = ? AND action = 'GRADE_DRAFT_CREATED'",
                Long.class, ENROLLMENT_ID
        )).isEqualTo(1L);
    }

    @Test
    void updatesCompleteScoresCalculatesGradeAndFinalizes() {
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
        gradeManagementService.createDraft(
                new GradeSaveRequestDTO(CLASS_ID, List.of(new GradeScoreRequestDTO(
                        ENROLLMENT_ID, new BigDecimal("90"), null, null, null
                ))),
                "grade-create-before-update", professor, "trace-create", "127.0.0.1"
        );

        var updated = gradeManagementService.updateDraft(
                new GradeSaveRequestDTO(CLASS_ID, List.of(new GradeScoreRequestDTO(
                        ENROLLMENT_ID, new BigDecimal("90"), new BigDecimal("80"),
                        new BigDecimal("100"), new BigDecimal("95")
                ))),
                "grade-update-complete", professor, "trace-update", "127.0.0.1"
        );
        var finalized = gradeManagementService.finalizeGrades(
                CLASS_ID, new GradeFinalizeRequestDTO(GradeStatus.OPENED),
                "grade-finalize", professor, "trace-finalize", "127.0.0.1"
        );

        assertThat(updated.data().grades().getFirst().totalScore()).isEqualByComparingTo("90.00");
        assertThat(updated.data().grades().getFirst().letterGrade()).isEqualTo("A");
        assertThat(updated.data().grades().getFirst().gradeStatus()).isEqualTo(GradeStatus.DRAFT);
        assertThat(finalized.data().grades().getFirst().gradeStatus()).isEqualTo(GradeStatus.OPENED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' "
                        + "AND target_id = ? AND action = 'GRADE_FINALIZED'",
                Long.class, ENROLLMENT_ID
        )).isEqualTo(1L);
    }

    @Test
    void blocksFinalizationWhenOneOrMoreScoresAreMissing() {
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
        gradeManagementService.createDraft(
                new GradeSaveRequestDTO(CLASS_ID, List.of(new GradeScoreRequestDTO(
                        ENROLLMENT_ID, new BigDecimal("90"), null, null, null
                ))),
                "grade-create-incomplete", professor, "trace-create", "127.0.0.1"
        );

        assertThatThrownBy(() -> gradeManagementService.finalizeGrades(
                CLASS_ID, new GradeFinalizeRequestDTO(GradeStatus.OPENED),
                "grade-finalize-incomplete", professor, "trace-finalize", "127.0.0.1"
        )).isInstanceOf(InvalidGradeManagementRequestException.class)
                .hasMessageContaining("네 가지 점수");
    }

    @Test
    void blocksProfessorWhoDoesNotOwnTheLecture() {
        assertThatThrownBy(() -> gradeManagementService.getGrades(
                CLASS_ID, new CurrentUser(99603L, "PROFESSOR")
        )).isInstanceOf(GradeManagementAccessDeniedException.class);
    }

    @Test
    void blocksDraftOutsideGradeEntryPeriod() {
        jdbcTemplate.update("UPDATE grade_operation_periods "
                        + "SET start_date = ?, end_date = ? "
                        + "WHERE semester_id = ? AND operation_type = 'GRADE_ENTRY'",
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), 99601L);

        GradeSaveRequestDTO request = new GradeSaveRequestDTO(CLASS_ID, List.of(
                new GradeScoreRequestDTO(
                        ENROLLMENT_ID, new BigDecimal("90.00"), null, null, null
                )
        ));

        assertThatThrownBy(() -> gradeManagementService.createDraft(
                request, "grade-entry-outside", new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR"),
                "trace-outside", "127.0.0.1"
        )).isInstanceOf(GradeManagementConflictException.class)
                .hasMessage("성적입력 기간이 아닙니다.");
    }
}
