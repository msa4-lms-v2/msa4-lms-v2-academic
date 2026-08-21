package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AdmissionCandidateStateConflictException extends BusinessException {

    public AdmissionCandidateStateConflictException(AdmissionCandidateStatus current,
                                                    AdmissionCandidateStatus target) {
        super(CustomResponseCode.DUPLICATE_DATA,
                current + " 상태에서 " + target + " 상태로 변경할 수 없습니다.");
    }

    public AdmissionCandidateStateConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
