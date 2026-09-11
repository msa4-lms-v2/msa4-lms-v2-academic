package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.response.AdminInfoChangeRequestSummaryResponseDTO;
import java.util.List;

public record AdminInfoChangeRequestSearchResult(
        List<AdminInfoChangeRequestSummaryResponseDTO> items,
        long totalCount
) {
}
