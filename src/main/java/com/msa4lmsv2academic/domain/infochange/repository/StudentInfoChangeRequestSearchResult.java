package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import java.util.List;

public record StudentInfoChangeRequestSearchResult(
        List<StudentInfoChangeRequest> items,
        long totalCount
) {
}
