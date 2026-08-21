package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import java.util.List;

public record GraduationRequirementSearchResult(
        List<GraduationRequirement> items,
        long totalCount
) {
}
