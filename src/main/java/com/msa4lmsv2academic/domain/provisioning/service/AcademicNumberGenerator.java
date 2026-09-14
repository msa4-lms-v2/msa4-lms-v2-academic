package com.msa4lmsv2academic.domain.provisioning.service;

import com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException;
import java.util.Locale;

/** 연도 2자리 + 학과 ID 2자리 + 학생/교수 PK의 끝 4자리. */
public final class AcademicNumberGenerator {
    private AcademicNumberGenerator() {}

    public static String departmentCode(Long departmentId) {
        if (departmentId == null || departmentId < 1 || departmentId > 99) {
            throw new InvalidAdmissionCandidateRequestException("학번·교번 발급에는 1~99 범위의 학과 ID가 필요합니다.");
        }
        return String.format(Locale.ROOT, "%02d", departmentId);
    }

    public static String generate(Short year, Long departmentId, Long entityId) {
        if (year == null || year < 1900 || entityId == null || entityId < 1) {
            throw new InvalidAdmissionCandidateRequestException("연도는 1900년 이상이고 학생/교수 ID는 양수여야 합니다.");
        }
        long sequence = entityId % 10_000;
        return String.format(Locale.ROOT, "%02d%s%04d", year % 100, departmentCode(departmentId), sequence);
    }
}
