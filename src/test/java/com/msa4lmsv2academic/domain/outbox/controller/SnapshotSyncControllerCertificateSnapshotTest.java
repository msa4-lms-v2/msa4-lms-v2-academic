package com.msa4lmsv2academic.domain.outbox.controller;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import com.msa4lmsv2academic.global.security.CurrentUser;

// Deliberately NOT @Transactional - the whole point is to let the controller's own
// @Transactional actually commit, which is exactly where UnexpectedRollbackException used to
// surface for a student with no matching graduation_requirements row (a @Transactional test
// method never reaches that commit, so it couldn't have caught this).
@SpringBootTest
class SnapshotSyncControllerCertificateSnapshotTest extends MySqlIntegrationTest {

    private static final long STUDENT_ID = 93001L;
    private static final long STUDENT_USER_ID = 93001L;

    @Autowired
    private SnapshotSyncController snapshotSyncController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (93001, 'FIX-COL', '검증대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (93001, '239', 93001, '검증학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '검증학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        // admission_year 2024, deliberately no matching graduation_requirements row - the exact
        // condition that used to throw UnexpectedRollbackException at commit time.
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 93001, 2, 2024, 'ENROLLED', NULL)",
                STUDENT_ID, STUDENT_USER_ID);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void certificateSnapshotCommitsCleanlyWhenGraduationRequirementIsMissing() {
        assertThatCode(() -> snapshotSyncController.getStudentCertificateSnapshot(STUDENT_ID,
                new CurrentUser(STUDENT_USER_ID, "STUDENT")))
                .doesNotThrowAnyException();
    }
}
