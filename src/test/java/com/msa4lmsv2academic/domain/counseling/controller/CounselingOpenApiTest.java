package com.msa4lmsv2academic.domain.counseling.controller;

import static org.hamcrest.Matchers.hasItems;
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
class CounselingOpenApiTest extends MySqlIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsOnlineCounselingContracts() throws Exception {
        String collection = "$['paths']['/api/academic/counseling']";
        String detail = "$['paths']['/api/academic/counseling/{counselingId}']";
        String answer = "$['paths']['/api/academic/counseling/{counselingId}/answer']";
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collection + "['get']").exists())
                .andExpect(jsonPath(collection + "['post']").exists())
                .andExpect(jsonPath(detail + "['get']").exists())
                .andExpect(jsonPath(answer + "['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/counseling/availability']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/academic/counseling/appointments']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['CounselingCreateRequestDTO']['required']")
                        .value(hasItems("professorId", "title", "question")))
                .andExpect(jsonPath("$['components']['schemas']['CounselingAnswerRequestDTO']['required']")
                        .value(hasItems("answer")));
    }
}
