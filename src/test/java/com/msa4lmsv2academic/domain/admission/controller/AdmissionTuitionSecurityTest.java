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
    void paymentAndAuthTokensCannotImpersonateEachOther() throws Exception {
        var service = mock(AdmissionTuitionService.class);
        var provisioning = mock(AccountProvisioningService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionTuitionController(service, provisioning, "payment-test", "auth-test"))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/academic/internal/admissions/7/paid")
                .header("X-Admission-Token", "auth-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tuitionBillId\":100}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/academic/internal/admissions/7/activated")
                .header("X-Admission-Token", "payment-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service, provisioning);
    }

    @Test
    void missingServerTokensFailClosed() throws Exception {
        var service = mock(AdmissionTuitionService.class);
        var provisioning = mock(AccountProvisioningService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdmissionTuitionController(service, provisioning, "", ""))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/academic/internal/admissions/7").header("X-Admission-Token", ""))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service, provisioning);
    }
}
