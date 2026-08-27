package com.msa4lmsv2academic.domain.academicstatus.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AcademicStatusHistoryOpenApiTest {

    // 공통 MySqlIntegrationTest의 UI 비활성화 설정과 분리해 실제 UI 리소스도 검증한다.
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("lms_academic").withUsername("academic").withPassword("academic");

    static {
        MYSQL.start();
    }

    @Autowired private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
    void generatedContractDocumentsFiltersPermissionsFieldsAndErrors() throws Exception {
        String path = "$['paths']['/api/academic/status-histories']";
        String operation = path + "['get']";
        String schema = "$['components']['schemas']['AcademicStatusHistoryResponseDTO']";
        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath(operation + "['operationId']").value("searchAcademicStatusHistories"))
                .andExpect(jsonPath(operation + "['operationId']").value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(operation + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(operation + "['description']").value(containsString("현재 지도학생")))
                .andExpect(jsonPath(operation + "['parameters'][*]['name']", hasItems(
                        "page", "size", "keyword", "studentId", "departmentId", "previousStatus", "newStatus",
                        "sourceType", "fromDate", "toDate", "sortDirection")))
                .andExpect(jsonPath(operation + "['responses']['200']").exists())
                .andExpect(jsonPath(operation + "['responses']['400']").exists())
                .andExpect(jsonPath(operation + "['responses']['401']").exists())
                .andExpect(jsonPath(operation + "['responses']['403']").exists())
                .andExpect(jsonPath(operation + "['responses']['404']").exists())
                .andExpect(jsonPath(schema + "['required']", hasItems("historyId", "studentId", "studentName",
                        "departmentId", "departmentName", "previousStatus", "newStatus", "reason", "changedBy",
                        "sourceType", "sourceId", "createdAt")))
                .andExpect(jsonPath(schema + "['properties']['reason']['maxLength']").value(500))
                .andExpect(jsonPath(schema + "['properties']['sourceType']['enum']", hasItems(
                        "LEAVE_REQUEST", "WITHDRAWAL_REQUEST", "DISMISSAL", "ADMIN_CORRECTION", "READMISSION")))
                .andExpect(jsonPath(schema + "['properties']['createdAt']['format']").value("date-time"))
                .andExpect(jsonPath(schema + "['properties']['email']").doesNotExist())
                .andExpect(jsonPath(schema + "['properties']['changedByName']").doesNotExist())
                .andExpect(jsonPath(path + "['post']").doesNotExist())
                .andExpect(jsonPath(path + "['patch']").doesNotExist())
                .andExpect(jsonPath(path + "['delete']").doesNotExist());
    }

    @Test
    void swaggerUiAndDocumentConfigurationAreAvailable() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api-docs"));
    }
}
