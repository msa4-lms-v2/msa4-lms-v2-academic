package com.msa4lmsv2academic.domain.lecture.repository;

import java.util.List;

public record ProfessorLectureSearchResult(
        List<ProfessorLectureQueryResult> items,
        long totalCount
) {
}
