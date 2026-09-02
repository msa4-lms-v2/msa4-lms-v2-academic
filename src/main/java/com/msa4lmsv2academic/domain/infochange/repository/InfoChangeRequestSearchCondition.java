package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import org.springframework.data.domain.Sort;

public record InfoChangeRequestSearchCondition(
        String keyword,
        InfoChangeRequestStatus status,
        Long departmentId,
        Long requesterUserId,
        Sort.Direction sortDirection,
        long offset,
        int limit
) {
}
