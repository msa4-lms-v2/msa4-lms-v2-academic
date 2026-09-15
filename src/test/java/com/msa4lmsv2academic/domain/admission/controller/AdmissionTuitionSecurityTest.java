package com.msa4lmsv2academic.domain.admission.controller;

import com.msa4lmsv2academic.domain.admission.service.AdmissionTuitionService;
import com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService;
import com.msa4lmsv2academic.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdmissionTuitionSecurityTest {
    @Test
    void trustedInternalRequestsDoNotRequireAdmissionTokens() throws Exception {
        var service = mock(AdmissionTuitionService.class);
        var provisioning = mock(AccountProvisioningService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionTuitionController(service, provisioning))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/academic/internal/admissions/7")).andExpect(status().isOk());
        mvc.perform(post("/api/academic/internal/admissions/7/bill")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tuitionBillId\":100}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/academic/internal/admissions/7/paid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tuitionBillId\":100}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/academic/internal/admissions/7/activated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":1}"))
                .andExpect(status().isOk());
        verify(service).get(7L);
        verify(service).bind(7L, 100L);
        verify(service).paid(7L, 100L);
        verify(service).activated(7L, 1L);
    }

    @Test
    void invalidBusinessIdentifiersAreStillRejectedWithoutTokens() throws Exception {
        var service = mock(AdmissionTuitionService.class);
        var provisioning = mock(AccountProvisioningService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionTuitionController(service, provisioning))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/academic/internal/admissions/7/paid")
                .contentType(MediaType.APPLICATION_JSON).content("{\"tuitionBillId\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/academic/internal/admissions/7/activated")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, provisioning);
    }

    @Test
    void studentCreationStillRequiresMatchingCandidateId() throws Exception {
        var service = mock(AdmissionTuitionService.class);
        var provisioning = mock(AccountProvisioningService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionTuitionController(service, provisioning))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var body = """
                {"userId":1,"name":"테스트 학생","birthDate":"2008-01-01",
                 "email":"student@example.invalid","departmentId":1,"admissionYear":2027,
                 "admissionCandidateId":8,"advisorProfessorId":1}
                """;
        mvc.perform(post("/api/academic/internal/admissions/7/student")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        verifyNoInteractions(service, provisioning);
        mvc.perform(post("/api/academic/internal/admissions/8/student")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(provisioning).provisionStudent(argThat(request -> request.admissionCandidateId().equals(8L)));
    }
}
