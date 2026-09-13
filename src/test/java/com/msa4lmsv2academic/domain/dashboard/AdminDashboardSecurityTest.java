package com.msa4lmsv2academic.domain.dashboard;

import com.msa4lmsv2academic.global.security.*;
import com.msa4lmsv2academic.domain.outbox.controller.SnapshotSyncController;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.student.repository.StudentRepository;
import com.msa4lmsv2academic.domain.withdrawal.repository.WithdrawalRequestRepository;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.time.LocalDate;
import java.util.Optional;
import com.msa4lmsv2academic.global.security.filter.GatewayHeaderAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(AdminDashboardSecurityTest.Config.class)
class AdminDashboardSecurityTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, GatewayHeaderAuthenticationFilter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, AdminDashboardController.class, SnapshotSyncController.class})
    static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean AdminDashboardService service() { return mock(AdminDashboardService.class); }
        @Bean SemesterRepository semesters() { return mock(SemesterRepository.class); }
        @Bean StudentRepository students() { return mock(StudentRepository.class); }
        @Bean WithdrawalRequestRepository withdrawals() { return mock(WithdrawalRequestRepository.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired AdminDashboardService service;
    @Autowired SemesterRepository semesters;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(service);
        reset(semesters);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(service.getDashboard()).thenReturn(new AdminDashboardResponse(null, new AdminDashboardResponse.Summary(0,0), java.util.List.of(), null));
    }
    @Test void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/academic/admin/dashboard")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"STUDENT","PROFESSOR"})
    void otherRolesAreForbidden(String role) throws Exception {
        mvc.perform(get("/api/academic/admin/dashboard").header("X-User-Id","1").header("X-User-Role",role))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminGetsFrontendContract() throws Exception {
        mvc.perform(get("/api/academic/admin/dashboard").header("X-User-Id","1").header("X-User-Role","ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.summary.lecturePending").value(0))
                .andExpect(jsonPath("$.data.tasks").isArray());
        verify(service).getDashboard();
    }

    @Test void currentSnapshotUsesExactRouteAndReturnsSuccessfulNullWhenUnset() throws Exception {
        when(semesters.findFirstByCurrentTrue()).thenReturn(Optional.empty());
        mvc.perform(get("/api/academic/catalog/semesters/current/snapshot"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test void currentSnapshotReturnsConfiguredSemesterEvenOutsideItsDates() throws Exception {
        var start = LocalDate.of(2030, 9, 1);
        var semester = Semester.create((short)2030, SemesterTerm.SECOND, start, start.plusMonths(4),
                start.minusMonths(1).atStartOfDay(), start.atStartOfDay(), true);
        org.springframework.test.util.ReflectionTestUtils.setField(semester, "id", 20L);
        when(semesters.findFirstByCurrentTrue()).thenReturn(Optional.of(semester));
        mvc.perform(get("/api/academic/catalog/semesters/current/snapshot"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.year").value(2030))
                .andExpect(jsonPath("$.data.label").value("2학기"));
    }
}
