package com.msa4lmsv2academic.domain.withdrawal.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"academic.enrollment.idempotency-cleanup.cron=-", "academic.withdrawal.idempotency-cleanup.cron=-"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WithdrawalSwaggerUiTest {
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("withdrawal_swagger").withUsername("test").withPassword("test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        // local 문서 설정은 그대로 사용하되 업무 더미는 실행하지 않습니다.
        registry.add("spring.sql.init.data-locations", () -> "optional:classpath:test-data.sql");
    }

    @Autowired private MockMvc mvc;

    @Test
    void localSwaggerUiAndItsDocumentEndpointAreExposed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Swagger UI")));
        mvc.perform(get("/api-docs/swagger-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/api-docs"));
        mvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/withdrawals/{withdrawalId}/status']['patch']['operationId']")
                        .value("cancelWithdrawal"));
    }
}
