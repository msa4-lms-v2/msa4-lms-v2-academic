package com.msa4lmsv2academic.domain.admission.repository;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import java.util.List;

public record AdmissionCandidateSearchResult(List<AdmissionCandidate> items, long totalCount) {
}
