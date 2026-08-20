package com.msa4lmsv2academic.domain.lecture.controller;

import static org.hamcrest.Matchers.containsString;
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
class LectureSyllabusOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsLectureSyllabusContract() throws Exception {
        String syllabusPath = "$['paths']['/api/academic/classes/{classId}/syllabus']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(syllabusPath + "['get']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['200']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['400']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['401']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['403']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['404']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['responses']['409']").exists())
                .andExpect(jsonPath(syllabusPath + "['put']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['LectureSyllabusUpdateRequestDTO']"
                        + "['properties']['syllabus']").exists())
                .andExpect(jsonPath("$['components']['schemas']['LectureSyllabusResponseDTO']"
                        + "['properties']['status']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
