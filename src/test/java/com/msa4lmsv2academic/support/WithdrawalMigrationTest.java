package com.msa4lmsv2academic.support;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

class WithdrawalMigrationTest {
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("withdrawal_migration").withUsername("test").withPassword("test");
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
        // 이 테스트 전용 컨테이너의 네 테이블만 초기화합니다.
        for (String table : new String[]{"withdrawal_requests", "idempotency_keys", "students", "users"}) {
            jdbc.execute("DROP TABLE IF EXISTS " + table);
        }
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE students (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE, "
                + "FOREIGN KEY (user_id) REFERENCES users(id)) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE withdrawal_requests (id BIGINT PRIMARY KEY, status VARCHAR(20) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbc.execute("""
                CREATE TABLE idempotency_keys (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  idempotency_key VARCHAR(100) NOT NULL,
                  requester_student_id BIGINT NOT NULL,
                  endpoint VARCHAR(255) NOT NULL,
                  request_hash VARCHAR(64) NOT NULL,
                  response_snapshot JSON NULL,
                  status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  expires_at DATETIME NOT NULL,
                  UNIQUE KEY uk_idempotency_keys_key (idempotency_key),
                  INDEX idx_idempotency_keys_requester (requester_student_id),
                  INDEX idx_idempotency_keys_expiry (status, expires_at),
                  CONSTRAINT fk_idempotency_keys_requester FOREIGN KEY (requester_student_id) REFERENCES students(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                """);
        jdbc.update("INSERT INTO users VALUES (1), (9)");
        jdbc.update("INSERT INTO students VALUES (1,9), (2,1)");
        jdbc.update("""
                INSERT INTO idempotency_keys (idempotency_key,requester_student_id,endpoint,request_hash,response_snapshot,status,expires_at)
                VALUES ('Keep-Case',1,'POST /api/academic/enrollments',?, '{"code":"00","data":{"studentId":1}}','COMPLETED','2099-01-01')
                """, "a".repeat(64));
        jdbc.update("INSERT INTO idempotency_keys (idempotency_key,requester_student_id,endpoint,request_hash,status,expires_at) "
                + "VALUES ('second',2,'POST /api/academic/enrollments',?,'IN_PROGRESS','2099-01-01')", "b".repeat(64));
    }

    @Test
    void convertsDifferentAndOverlappingIdsAndPreservesKeysSnapshotsAndCollation() throws Exception {
        String snapshot = jdbc.queryForObject("SELECT response_snapshot FROM idempotency_keys WHERE idempotency_key='Keep-Case'", String.class);
        migrate();
        assertThat(jdbc.queryForList("SELECT requester_user_id FROM idempotency_keys ORDER BY id", Long.class)).containsExactly(9L, 1L);
        assertThat(jdbc.queryForObject("SELECT response_snapshot FROM idempotency_keys WHERE idempotency_key='Keep-Case'", String.class))
                .isEqualTo(snapshot);
        assertThat(jdbc.queryForObject("SELECT request_hash FROM idempotency_keys WHERE idempotency_key='Keep-Case'", String.class))
                .isEqualTo("a".repeat(64));
        assertThat(jdbc.queryForObject("SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='idempotency_keys' AND COLUMN_NAME='idempotency_key'", String.class)).isEqualTo("utf8mb4_0900_ai_ci");
        assertThat(columnCount("idempotency_keys", "requester_student_id")).isZero();
        assertThat(columnCount("withdrawal_requests", "cancel_reason")).isEqualTo(1);
        assertThat(columnCount("withdrawal_requests", "cancelled_at")).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO withdrawal_requests (id,status,cancelled_by) VALUES (1,'CANCELLED',999)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        migrate();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_keys", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT requester_user_id FROM idempotency_keys WHERE idempotency_key='Keep-Case'", Long.class)).isEqualTo(9L);
    }

    @Test
    void orphanMappingStopsBeforeDroppingOrAddingRequesterColumns() {
        jdbc.execute("ALTER TABLE idempotency_keys DROP FOREIGN KEY fk_idempotency_keys_requester");
        jdbc.update("UPDATE idempotency_keys SET requester_student_id=999 WHERE idempotency_key='Keep-Case'");
        assertThatThrownBy(this::migrate).isInstanceOf(SQLException.class).hasMessageContaining("Unmapped student requester");
        assertThat(columnCount("idempotency_keys", "requester_student_id")).isEqualTo(1);
        assertThat(columnCount("idempotency_keys", "requester_user_id")).isZero();
    }

    @Test
    void mismatchedPartialMigrationIsNotSilentlyOverwritten() {
        jdbc.execute("ALTER TABLE idempotency_keys ADD COLUMN requester_user_id BIGINT NULL");
        jdbc.update("UPDATE idempotency_keys SET requester_user_id=1 WHERE idempotency_key='Keep-Case'");
        assertThatThrownBy(this::migrate).isInstanceOf(SQLException.class).hasMessageContaining("Requester mapping mismatch");
        assertThat(columnCount("idempotency_keys", "requester_student_id")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT requester_user_id FROM idempotency_keys WHERE idempotency_key='Keep-Case'", Long.class)).isEqualTo(1L);
    }

    @Test
    void interruptedMappingCanBeResumed() throws Exception {
        jdbc.execute("ALTER TABLE idempotency_keys ADD COLUMN requester_user_id BIGINT NULL");
        jdbc.update("UPDATE idempotency_keys SET requester_user_id=9 WHERE idempotency_key='Keep-Case'");
        migrate();
        assertThat(jdbc.queryForList("SELECT requester_user_id FROM idempotency_keys ORDER BY id", Long.class)).containsExactly(9L, 1L);
    }

    @Test
    void unexpectedFkStopsForInspection() {
        jdbc.execute("ALTER TABLE idempotency_keys DROP FOREIGN KEY fk_idempotency_keys_requester");
        assertThatThrownBy(this::migrate).isInstanceOf(SQLException.class).hasMessageContaining("Unexpected requester FK");
        assertThat(columnCount("idempotency_keys", "requester_student_id")).isEqualTo(1);
    }

    private int columnCount(String table, String column) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME=? AND COLUMN_NAME=?", Integer.class, table, column);
    }

    private void migrate() throws Exception {
        String script = new ClassPathResource("migration/20260827_withdrawal_cancellation_and_user_idempotency.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("(?m)^DELIMITER.*$", "");
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement()) {
            for (String sql : script.split("\\$\\$")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }
}

