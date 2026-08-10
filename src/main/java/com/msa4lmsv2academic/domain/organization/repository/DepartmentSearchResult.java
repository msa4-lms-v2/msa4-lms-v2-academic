package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.Department;

import java.util.List;

public record DepartmentSearchResult(List<Department> items, long totalCount) {
}
