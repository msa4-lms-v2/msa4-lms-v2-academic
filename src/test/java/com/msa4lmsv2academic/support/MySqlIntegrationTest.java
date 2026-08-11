package com.msa4lmsv2academic.support;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

import javax.crypto.SecretKey;

public abstract class MySqlIntegrationTest {

    public static final String JWT_SECRET = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("academic-test-jwt-secret-key-that-is-longer-than-thirty-two-bytes"
                    .getBytes(StandardCharsets.UTF_8));

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
        registry.add("spring.sql.init.data-locations", () -> "optional:classpath:test-data.sql");
        registry.add("jwt.secret", () -> JWT_SECRET);
        registry.add("springdoc.api-docs.enabled", () -> "true");
        registry.add("springdoc.api-docs.path", () -> "/api-docs");
        registry.add("springdoc.swagger-ui.enabled", () -> "false");
    }

    protected static SecretKey jwtSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(JWT_SECRET));
    }
}
