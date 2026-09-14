package com.msa4lmsv2academic.domain.admission.controller;
import com.msa4lmsv2academic.domain.admission.service.AdmissionTuitionService;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateDetailResponseDTO;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@RestController @RequestMapping("/api/academic/internal/admissions")
public class AdmissionTuitionController {
    private final AdmissionTuitionService service;
    private final com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService provisioning;
    private final String paymentToken; private final String authToken;
    public AdmissionTuitionController(AdmissionTuitionService service, com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService provisioning,
            @Value("${services.admission.payment-token:${ADMISSION_PAYMENT_TOKEN:}}") String paymentToken, @Value("${services.admission.auth-token:${ADMISSION_AUTH_TOKEN:}}") String authToken) {
        this.service=service;this.provisioning=provisioning; this.paymentToken=paymentToken; this.authToken=authToken;
    }
    private boolean matches(String actual,String expected) {
        return !expected.isBlank() && actual!=null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8));
    }
    private void verify(String actual,String expected) { if(!matches(actual,expected)) throw new org.springframework.security.access.AccessDeniedException("입학 서비스 인증이 필요합니다."); }
    @GetMapping("/{id}") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> get(@PathVariable Long id,@RequestHeader("X-Admission-Token") String token) {
        if(!matches(token,paymentToken)) verify(token,authToken); return GlobalResponseDTO.success(service.get(id));
    }
    public record BillRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long tuitionBillId) {}
    @PostMapping("/{id}/bill") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> bind(@PathVariable Long id,@RequestHeader("X-Admission-Token") String token,@jakarta.validation.Valid @RequestBody BillRequest body) {
        verify(token,paymentToken); return GlobalResponseDTO.success(service.bind(id,body.tuitionBillId()));
    }
    @PostMapping("/{id}/paid") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> paid(@PathVariable Long id,@RequestHeader("X-Admission-Token") String token,@jakarta.validation.Valid @RequestBody BillRequest body) {
        verify(token,paymentToken); return GlobalResponseDTO.success(service.paid(id,body.tuitionBillId()));
    }
    @PostMapping("/{id}/student") public GlobalResponseDTO<com.msa4lmsv2academic.domain.provisioning.response.StudentProvisioningResponseDTO> student(@PathVariable Long id,@RequestHeader("X-Admission-Token") String token,@jakarta.validation.Valid @RequestBody com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO request) {
        verify(token,authToken);
        if(!id.equals(request.admissionCandidateId()))throw new com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException("입학 예정자 ID가 일치하지 않습니다.");
        return GlobalResponseDTO.success(provisioning.provisionStudent(request));
    }
    public record ActivatedRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long accountId) {}
    @PostMapping("/{id}/activated") public GlobalResponseDTO<Void> activated(@PathVariable Long id,@RequestHeader("X-Admission-Token") String token,@jakarta.validation.Valid @RequestBody ActivatedRequest body) {
        verify(token,authToken); service.activated(id,body.accountId()); return GlobalResponseDTO.success();
    }
}
