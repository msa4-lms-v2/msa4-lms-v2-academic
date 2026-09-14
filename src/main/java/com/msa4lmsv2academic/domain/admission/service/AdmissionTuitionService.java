package com.msa4lmsv2academic.domain.admission.service;
import com.msa4lmsv2academic.domain.admission.entity.*;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository;
import com.msa4lmsv2academic.domain.admission.response.AdmissionCandidateDetailResponseDTO;
import com.msa4lmsv2academic.domain.outbox.service.OutboxEventService;
import com.msa4lmsv2academic.global.error.*;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class AdmissionTuitionService {
    private final AdmissionCandidateRepository candidates;
    private final OutboxEventService outbox;
    public AdmissionCandidateDetailResponseDTO get(Long id) { return AdmissionCandidateDetailResponseDTO.from(find(id)); }
    public AdmissionCandidateDetailResponseDTO bind(Long id, Long billId) {
        var c=find(id); c.bindTuitionBill(billId); return AdmissionCandidateDetailResponseDTO.from(c);
    }
    public AdmissionCandidateDetailResponseDTO paid(Long id, Long billId) {
        var c=find(id);
        if(c.getStatus()==AdmissionCandidateStatus.CANCELLED) return AdmissionCandidateDetailResponseDTO.from(c);
        if(c.getAdvisorProfessorId()==null) throw new AdmissionCandidateStateConflictException("지도교수 보완이 필요합니다.");
        if(c.confirmTuitionPaid(billId)) {
            Map<String,Object> p=new LinkedHashMap<>();
            p.put("admissionCandidateId",id); p.put("administratorId",c.getCreatedBy().getId());
            p.put("name",c.getName()); p.put("birthDate",c.getBirthDate().toString()); p.put("email",c.getEmail());
            p.put("phoneNumber",c.getPhoneNumber()); p.put("address",c.getAddress());
            p.put("departmentId",c.getDepartment().getId()); p.put("advisorProfessorId",c.getAdvisorProfessorId());
            p.put("admissionYear",c.getAdmissionYear());
            outbox.record("ADMISSION_CANDIDATE",id,"AdmissionCandidateRegistered",p,1L);
        }
        return AdmissionCandidateDetailResponseDTO.from(c);
    }
    public void activated(Long id,Long accountId) { find(id).completeRegistration(accountId); }
    private AdmissionCandidate find(Long id) { return candidates.findByIdForUpdate(id).orElseThrow(AdmissionCandidateNotFoundException::new); }
}
