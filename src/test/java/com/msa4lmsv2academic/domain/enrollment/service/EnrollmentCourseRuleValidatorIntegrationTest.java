package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditQueryRepository;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentPrerequisiteRetakeNotAllowedException;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EnrollmentCourseRuleValidatorIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_ID = 99001L;
    private static final long SEMESTER_ID = 99001L;
    private static final long TARGET_LECTURE_ID = 99009L;

    @Autowired
    private EnrollmentCourseRuleValidator validator;

    @Autowired
    private EnrollmentCreditQueryRepository creditQueryRepository;

    @Autowired
    private EnrollmentCreditLimitRuleRepository ruleRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (99001, 'VLD-COL', '검증대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99001, '990', 99001, '검증학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES "
                + "(99011, '검증학생', 'STUDENT', 'ACTIVE'), "
                + "(99012, '다른학생', 'STUDENT', 'ACTIVE'), "
                + "(99013, '검증교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99001, 0, 99013, 2020, 99001)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) VALUES "
                + "(99001, 99011, 99001, 3, 2088, 'ENROLLED', 99001), "
                + "(99002, 99012, 99001, 3, 2088, 'ENROLLED', 99001)");
        insertSemester(SEMESTER_ID, "SECOND");
        insertSemester(99002L, "FIRST");
        jdbcTemplate.update("INSERT INTO enrollment_credit_limit_rules "
                + "(id, semester_id, max_credits, is_active) VALUES (99001, 99001, 9, 1)");
        insertCourse(99001L);
        insertCourse(99002L);
        insertCourse(99003L);
        insertCourse(99009L);
        insertLecture(99001L, 99001L, SEMESTER_ID, "01");
        insertLecture(99002L, 99002L, SEMESTER_ID, "01");
        insertLecture(99003L, 99003L, SEMESTER_ID, "01");
        insertLecture(99004L, 99002L, 99002L, "01");
        insertLecture(TARGET_LECTURE_ID, 99009L, SEMESTER_ID, "01");
        insertEnrollment(99001L, STUDENT_ID, 99001L, "ACTIVE", "OPENED", "F");
        insertEnrollment(99002L, STUDENT_ID, 99002L, "ACTIVE", "OPENED", "A");
        insertEnrollment(99003L, STUDENT_ID, 99003L, "CANCELLED", "DRAFT", null);
        insertEnrollment(99004L, STUDENT_ID, 99004L, "ACTIVE", "OPENED", "A");
        insertEnrollment(99005L, 99002L, 99003L, "ACTIVE", "DRAFT", null);
    }

    @Test
    void sumsAllActiveCreditsByStudentIdAndTargetSemesterNotEarnedCredits() {
        assertThat(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).isEqualTo(6L);
        assertThat(creditQueryRepository.sumActiveCredits(STUDENT_ID, 99002L)).isEqualTo(3L);
        assertThat(creditQueryRepository.sumActiveCredits(99002L, SEMESTER_ID)).isEqualTo(3L);
        assertThat(creditQueryRepository.sumActiveCredits(99002L, 99002L)).isZero();
        assertThat(creditQueryRepository.sumActiveCredits(99011L, SEMESTER_ID)).isZero();
    }

    @Test
    void sumsBeyondTinyintWithoutOverflowOrDistinctCreditValues() {
        for (int index = 0; index < 42; index++) {
            long id = 99100L + index;
            insertLecture(id, 99003L, SEMESTER_ID, "S" + index);
            insertEnrollment(id, STUDENT_ID, id, "ACTIVE", "DRAFT", null);
        }

        assertThat(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).isEqualTo(132L);
    }

    @Test
    void allowsExactLimitInNonCurrentSemesterWithoutSavingEnrollment() {
        Lecture targetLecture = entityManager.find(Lecture.class, TARGET_LECTURE_ID);
        Long beforeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM enrollments", Long.class);

        assertThat(targetLecture.getSemester().isCurrent()).isFalse();
        assertThatCode(() -> validator.validate(STUDENT_ID, targetLecture)).doesNotThrowAnyException();
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM enrollments", Long.class)).isEqualTo(beforeCount);
    }

    @Test
    void rejectsTotalAboveConfiguredLimit() {
        jdbcTemplate.update("UPDATE enrollment_credit_limit_rules SET max_credits = 8 WHERE id = 99001");

        assertCreditRejected(TARGET_LECTURE_ID, EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsInactiveRuleInsteadOfApplyingDefault() {
        jdbcTemplate.update("UPDATE enrollment_credit_limit_rules SET is_active = 0 WHERE id = 99001");

        assertThat(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID)).isEmpty();
        assertCreditRejected(TARGET_LECTURE_ID, EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_RULE_NOT_CONFIGURED);
    }

    @Test
    void rejectsMissingRuleForTargetSemester() {
        assertThat(ruleRepository.findBySemesterIdAndActiveTrue(99002L)).isEmpty();
        assertCreditRejected(99004L, EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_RULE_NOT_CONFIGURED);
    }

    @Test
    void rejectsMissingPrerequisiteEvenWhenCreditLimitIsSatisfied() {
        insertPrerequisite(99001L, 99003L, true);

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, entityManager.find(Lecture.class, TARGET_LECTURE_ID)))
                .isInstanceOfSatisfying(EnrollmentPrerequisiteRetakeNotAllowedException.class,
                        exception -> assertThat(exception.getReasons())
                                .containsExactly(PrerequisiteRetakeRuleRejectionReason.PREREQUISITE_NOT_COMPLETED));
    }

    @Test
    void usesOnlyActivePrerequisitesAndExistingPassingHistory() {
        insertPrerequisite(99001L, 99002L, true);
        insertPrerequisite(99002L, 99003L, false);

        assertThatCode(() -> validator.validate(STUDENT_ID, entityManager.find(Lecture.class, TARGET_LECTURE_ID)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"F,true", "C+,true", "B,false"})
    void reusesRetakePolicyAcrossSemesters(String grade, boolean isAllowed) {
        insertLecture(99008L, 99009L, 99002L, "01");
        insertEnrollment(99008L, STUDENT_ID, 99008L, "ACTIVE", "OPENED", grade);
        Lecture targetLecture = entityManager.find(Lecture.class, TARGET_LECTURE_ID);

        if (isAllowed) {
            assertThatCode(() -> validator.validate(STUDENT_ID, targetLecture)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> validator.validate(STUDENT_ID, targetLecture))
                    .isInstanceOfSatisfying(EnrollmentPrerequisiteRetakeNotAllowedException.class,
                            exception -> assertThat(exception.getReasons())
                                    .containsExactly(PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE));
        }
    }

    private void assertCreditRejected(long lectureId, EnrollmentCreditLimitRejectionReason reason) {
        assertThatThrownBy(() -> validator.validate(STUDENT_ID, entityManager.find(Lecture.class, lectureId)))
                .isInstanceOfSatisfying(EnrollmentCreditLimitNotAllowedException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(reason));
    }

    private void insertSemester(long id, String term) {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (?, 2090, ?, '2090-03-02', '2090-06-19', "
                + "'2090-02-01 09:00:00', '2090-02-07 18:00:00', 0)", id, term);
    }

    private void insertCourse(long id) {
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (?, 99001, ?, '검증교과목', 3, 3, 'MAJOR_REQUIRED')", id, "VLD-" + id);
    }

    private void insertLecture(long id, long courseId, long semesterId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio) "
                + "VALUES (?, ?, ?, 99001, ?, 40, 'OPEN', 30, 30, 30, 10)", id, semesterId, courseId, sectionNo);
    }

    private void insertEnrollment(long id, long studentId, long lectureId, String status, String gradeStatus, String grade) {
        jdbcTemplate.update("INSERT INTO enrollments "
                + "(id, student_id, lecture_id, status, enrolled_at, grade_status, letter_grade) "
                + "VALUES (?, ?, ?, ?, '2090-02-02 09:00:00', ?, ?)", id, studentId, lectureId, status, gradeStatus, grade);
    }

    private void insertPrerequisite(long id, long prerequisiteCourseId, boolean isActive) {
        jdbcTemplate.update("INSERT INTO course_prerequisites "
                + "(id, course_id, prerequisite_course_id, is_active) VALUES (?, 99009, ?, ?)", id, prerequisiteCourseId, isActive);
    }
}
