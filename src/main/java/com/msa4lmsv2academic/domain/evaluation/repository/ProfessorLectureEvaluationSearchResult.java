package com.msa4lmsv2academic.domain.evaluation.repository;

import java.util.List;

public record ProfessorLectureEvaluationSearchResult(
        List<ProfessorLectureEvaluationQueryResult> items,
        long totalCount
) {
}
