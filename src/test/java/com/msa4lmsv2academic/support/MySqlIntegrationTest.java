package com.msa4lmsv2academic.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

public abstract class MySqlIntegrationTest {

    public static final String JWT_SECRET = "academic-test-jwt-secret-key-that-is-longer-than-thirty-two-bytes";

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_academic")
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
        registry.add("security.jwt.secret", () -> JWT_SECRET);
        registry.add("springdoc.api-docs.enabled", () -> "true");
        registry.add("springdoc.api-docs.path", () -> "/api-docs");
        registry.add("springdoc.swagger-ui.enabled", () -> "false");
    }
}
