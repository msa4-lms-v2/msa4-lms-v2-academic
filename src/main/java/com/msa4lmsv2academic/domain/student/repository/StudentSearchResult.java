package com.msa4lmsv2academic.domain.student.repository;

import com.msa4lmsv2academic.domain.student.entity.Student;
import java.util.List;

public record StudentSearchResult(List<Student> items, long totalCount) {
}
