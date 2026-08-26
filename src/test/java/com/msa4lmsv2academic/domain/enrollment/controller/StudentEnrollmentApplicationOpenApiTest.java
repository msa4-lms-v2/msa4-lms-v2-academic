package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "academic.enrollment.idempotency-cleanup.cron=-")
@AutoConfigureMockMvc
class StudentEnrollmentApplicationOpenApiTest {
    // 공통 MySqlIntegrationTest는 UI를 강제로 비활성화하므로 UI 확인용 DB 설정을 분리합니다.
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_academic").withUsername("academic").withPassword("academic");

    static {
        MYSQL.start();
    }

    @Autowired private MockMvc mvc;

    @DynamicPropertySource
    static void swaggerUi(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.data-locations", () -> "optional:classpath:test-data.sql");
        registry.add("springdoc.api-docs.enabled", () -> "true");
        registry.add("springdoc.api-docs.path", () -> "/api-docs");
        registry.add("springdoc.swagger-ui.enabled", () -> "true");
    }

    @Test
    void generatedDocumentDescribesPostAndTypedErrorWithoutChangingExistingGet() throws Exception {
        String path = "$['paths']['/api/academic/enrollments']";
        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath(path + "['get']").exists())
                .andExpect(jsonPath(path + "['post']['operationId']").value("createMyEnrollment"))
                .andExpect(jsonPath(path + "['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['post']['parameters'][?(@.name == 'Idempotency-Key')].required", hasItem(true)))
                .andExpect(jsonPath(path + "['post']['requestBody']['required']").value(true))
                .andExpect(jsonPath(path + "['post']['responses']['200']").exists())
                .andExpect(jsonPath(path + "['post']['responses']['400']").exists())
                .andExpect(jsonPath(path + "['post']['responses']['401']").exists())
                .andExpect(jsonPath(path + "['post']['responses']['403']").exists())
                .andExpect(jsonPath(path + "['post']['responses']['404']").exists())
                .andExpect(jsonPath(path + "['post']['responses']['409']['content']['*/*']['schema']['$ref']")
                        .value("#/components/schemas/EnrollmentApplicationError"))
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentApplicationError']['properties']['data']['$ref']")
                        .value("#/components/schemas/EnrollmentApplicationErrorResponseDTO"))
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentApplicationErrorResponseDTO']['properties']['reasons']").exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentApplicationReasonResponseDTO']['properties']['code']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentCreateResponseDTO']['properties']['enrollmentId']").exists());
    }

    @Test
    void swaggerUiAndConfigurationAreServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api-docs"));
    }
}
