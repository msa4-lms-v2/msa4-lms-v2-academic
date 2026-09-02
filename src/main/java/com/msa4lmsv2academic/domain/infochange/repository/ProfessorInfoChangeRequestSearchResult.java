package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequest;
import java.util.List;

public record ProfessorInfoChangeRequestSearchResult(
        List<ProfessorInfoChangeRequest> items,
        long totalCount
) {
}
