package com.msa4lmsv2academic.domain.admission.controller;
import com.msa4lmsv2academic.domain.admission.service.AdmissionTuitionService;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateDetailResponseDTO;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/academic/internal/admissions")
public class AdmissionTuitionController {
    private final AdmissionTuitionService service;
    private final com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService provisioning;
    // 내부 서비스 신뢰 계약: SCG는 이 경로를 차단하고 Academic은 외부에 직접 노출하지 않는다.
    public AdmissionTuitionController(AdmissionTuitionService service, com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService provisioning) {
        this.service=service;this.provisioning=provisioning;
    }
    @GetMapping("/{id}") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> get(@PathVariable Long id) {
        return GlobalResponseDTO.success(service.get(id));
    }
    public record BillRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long tuitionBillId) {}
    @PostMapping("/{id}/bill") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> bind(@PathVariable Long id,@jakarta.validation.Valid @RequestBody BillRequest body) {
        return GlobalResponseDTO.success(service.bind(id,body.tuitionBillId()));
    }
    @PostMapping("/{id}/paid") public GlobalResponseDTO<AdmissionCandidateDetailResponseDTO> paid(@PathVariable Long id,@jakarta.validation.Valid @RequestBody BillRequest body) {
        return GlobalResponseDTO.success(service.paid(id,body.tuitionBillId()));
    }
    @PostMapping("/{id}/student") public GlobalResponseDTO<com.msa4lmsv2academic.domain.provisioning.response.StudentProvisioningResponseDTO> student(@PathVariable Long id,@jakarta.validation.Valid @RequestBody com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO request) {
        if(!id.equals(request.admissionCandidateId()))throw new com.msa4lmsv2academic.global.error.AdmissionCandidateStateConflictException("입학 예정자 ID가 일치하지 않습니다.");
        return GlobalResponseDTO.success(provisioning.provisionStudent(request));
    }
    public record ActivatedRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long accountId) {}
    @PostMapping("/{id}/activated") public GlobalResponseDTO<Void> activated(@PathVariable Long id,@jakarta.validation.Valid @RequestBody ActivatedRequest body) {
        service.activated(id,body.accountId()); return GlobalResponseDTO.success();
    }
}
