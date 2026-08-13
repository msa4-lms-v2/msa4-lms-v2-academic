package com.msa4lmsv2academic.domain.notice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NoticeOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsNoticeCrudContractsAndSchemas() throws Exception {
        String collectionPath = "$['paths']['/api/academic/catalog/notices']";
        String itemPath = "$['paths']['/api/academic/catalog/notices/{noticeId}']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']").exists())
                .andExpect(jsonPath(itemPath + "['get']").exists())
                .andExpect(jsonPath(itemPath + "['patch']").exists())
                .andExpect(jsonPath(itemPath + "['delete']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(itemPath + "['get']['responses']['403']").exists())
                .andExpect(jsonPath(itemPath + "['get']['responses']['404']").exists())
                .andExpect(jsonPath(itemPath + "['patch']['responses']['409']").exists())
                .andExpect(jsonPath(itemPath + "['delete']['responses']['409']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(collectionPath + "['post']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['patch']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['delete']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['NoticeCreateRequestDTO']['required']")
                        .value(hasItems("title", "targetRole")))
                .andExpect(jsonPath("$['components']['schemas']['NoticeSummaryResponseDTO']['properties']['content']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['NoticeDetailResponseDTO']['properties']['content']")
                        .exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
