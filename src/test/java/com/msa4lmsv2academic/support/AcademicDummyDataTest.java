package com.msa4lmsv2academic.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@Sql(scripts = {
        "classpath:dummy/01_msa4-lms-v2-academic-sample.sql",
        "classpath:dummy/01_msa4-lms-v2-academic-sample.sql"
})
class AcademicDummyDataTest {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_academic_dummy_test")
            .withUsername("academic")
            .withPassword("academic");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.data-locations", () -> "optional:classpath:test-data.sql");
        registry.add("springdoc.api-docs.enabled", () -> "false");
        registry.add("springdoc.swagger-ui.enabled", () -> "false");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loadsReusableSampleDataForImplementedAcademicFeatures() {
        assertThat(count("SELECT COUNT(*) FROM users WHERE id IN (1, 2, 3)"))
                .isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM departments WHERE code IN ('CSE', 'ELEC', 'KOR', 'OPEN')"))
                .isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM semesters WHERE is_current = TRUE"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM courses WHERE code LIKE 'CSE%' OR code LIKE 'GEN%'"))
                .isEqualTo(6);
        assertThat(count("""
                SELECT COUNT(*)
                FROM students student
                JOIN professors professor ON professor.id = student.advisor_id
                JOIN graduation_requirements requirement
                  ON requirement.department_id = student.department_id
                 AND requirement.admission_year = student.admission_year
                WHERE student.user_id = 1
                  AND professor.user_id = 2
                """))
                .isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*)
                FROM enrollments enrollment
                JOIN students student ON student.id = enrollment.student_id
                JOIN lectures lecture ON lecture.id = enrollment.lecture_id
                WHERE student.user_id = 1
                  AND lecture.semester_id = (
                      SELECT id
                      FROM semesters
                      WHERE academic_year = 2026 AND term = 'FIRST'
                  )
                """))
                .isEqualTo(6);
    }

    private int count(String sql) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }
}
