package com.msa4lmsv2academic.domain.provisioning.response;

// auth에 돌려줄 데이터
public record StudentProvisioningResponseDTO(
        Long userId,
        String loginId
) {
}
