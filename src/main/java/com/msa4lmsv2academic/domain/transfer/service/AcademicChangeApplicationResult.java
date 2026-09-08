package com.msa4lmsv2academic.domain.transfer.service;

import java.util.List;

public record AcademicChangeApplicationResult<T>(
        T response,
        boolean applied,
        List<String> replacedStoredNames
) {
    public AcademicChangeApplicationResult {
        replacedStoredNames = replacedStoredNames == null ? List.of() : List.copyOf(replacedStoredNames);
    }
}
