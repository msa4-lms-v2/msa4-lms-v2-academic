package com.msa4lmsv2academic.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.mysql.MySQLContainer;

class AcademicAffiliationMigrationTest {
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("academic_affiliation_migration").withUsername("test").withPassword("test");
    private JdbcTemplate jdbc;

    @BeforeAll
    static void startDatabase() {
        MYSQL.start();
    }

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    @BeforeEach
    void prepareLegacySchema() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        for (String table : new String[]{"academic_change_requests", "students", "majors", "departments"}) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("CREATE TABLE departments (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE majors (id BIGINT PRIMARY KEY, department_id BIGINT NOT NULL, "
                + "CONSTRAINT fk_majors_department FOREIGN KEY (department_id) REFERENCES departments(id)) ENGINE=InnoDB");
        jdbc.execute("""
                CREATE TABLE students (
                    id BIGINT PRIMARY KEY,
                    department_id BIGINT NOT NULL,
                    major_id BIGINT NULL,
                    double_major_id BIGINT NULL,
                    CONSTRAINT fk_students_department FOREIGN KEY (department_id) REFERENCES departments(id),
                    CONSTRAINT fk_students_major FOREIGN KEY (major_id) REFERENCES majors(id),
                    CONSTRAINT fk_students_double_major FOREIGN KEY (double_major_id) REFERENCES majors(id),
                    CONSTRAINT ck_students_distinct_majors
                        CHECK (major_id IS NULL OR double_major_id IS NULL OR major_id <> double_major_id),
                    INDEX idx_students_major_id (major_id),
                    INDEX idx_students_double_major_id (double_major_id)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE academic_change_requests (
                    id BIGINT PRIMARY KEY,
                    request_type VARCHAR(20) NOT NULL,
                    source_department_id BIGINT NOT NULL,
                    source_major_id BIGINT NULL,
                    target_department_id BIGINT NOT NULL,
                    target_major_id BIGINT NOT NULL,
                    target_semester_id BIGINT NULL,
                    request_period_id BIGINT NULL,
                    CONSTRAINT fk_academic_change_requests_source_major
                        FOREIGN KEY (source_major_id) REFERENCES majors(id),
                    CONSTRAINT fk_academic_change_requests_target_major
                        FOREIGN KEY (target_major_id) REFERENCES majors(id),
                    CONSTRAINT ck_academic_change_requests_target_scope CHECK (
                        (request_type = 'TRANSFER_DEPARTMENT' AND target_semester_id IS NOT NULL)
                        OR (request_type = 'DOUBLE_MAJOR' AND target_semester_id IS NULL AND request_period_id IS NOT NULL)
                    )
                ) ENGINE=InnoDB
                """);
        jdbc.update("INSERT INTO departments VALUES (10),(20),(30)");
        jdbc.update("INSERT INTO majors VALUES (100,10),(101,10),(200,20),(300,30)");
        jdbc.update("INSERT INTO students VALUES (1,10,100,200),(2,10,100,101)");
        jdbc.update("INSERT INTO academic_change_requests VALUES "
                + "(1,'TRANSFER_DEPARTMENT',10,100,20,200,1,NULL),"
                + "(2,'DOUBLE_MAJOR',10,100,30,300,NULL,1)");
    }

    @Test
    void migratesAffiliationsToDepartmentsAndRemovesMajorSchema() {
        new ResourceDatabasePopulator(
                new ClassPathResource("migration/20260902_align_academic_affiliations_with_department.sql"))
                .execute(jdbc.getDataSource());

        assertThat(tableCount("majors")).isZero();
        assertThat(columnCount("students", "major_id")).isZero();
        assertThat(columnCount("academic_change_requests", "source_major_id")).isZero();
        assertThat(columnCount("academic_change_requests", "target_major_id")).isZero();
        assertThat(jdbc.queryForObject("SELECT double_major_id FROM students WHERE id=1", Long.class)).isEqualTo(20L);
        assertThat(jdbc.queryForObject("SELECT double_major_id FROM students WHERE id=2", Long.class)).isNull();
        assertThatThrownBy(() -> jdbc.update("UPDATE students SET double_major_id=department_id WHERE id=1"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE students SET double_major_id=999 WHERE id=1"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private int tableCount(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME=?", Integer.class, table);
    }

    private int columnCount(String table, String column) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME=? AND COLUMN_NAME=?", Integer.class, table, column);
    }
}
