package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentApplicationRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationReasonResponseDTO;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class EnrollmentApplicationRejectedException extends BusinessException {
    private final List<EnrollmentApplicationReasonResponseDTO> reasons;

    public EnrollmentApplicationRejectedException(List<EnrollmentApplicationReasonResponseDTO> reasons) {
        super(CustomResponseCode.DUPLICATE_DATA,
                reasons.stream().map(EnrollmentApplicationReasonResponseDTO::message).collect(Collectors.joining(", ")));
        this.reasons = List.copyOf(reasons);
    }

    public static EnrollmentApplicationRejectedException from(EnrollmentApplicationRejectionReason reason) {
        return new EnrollmentApplicationRejectedException(List.of(
                EnrollmentApplicationReasonResponseDTO.from(reason.name(), reason.getMessage())));
    }
}
