package com.msa4lmsv2academic.domain.semester.repository;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import java.util.List;

public record SemesterSearchResult(List<Semester> items, long totalCount) {
}
