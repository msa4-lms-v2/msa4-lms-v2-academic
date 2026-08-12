package com.msa4lmsv2academic.domain.professor.repository;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import java.util.List;

public record ProfessorSearchResult(List<Professor> items, long totalCount) {
}
