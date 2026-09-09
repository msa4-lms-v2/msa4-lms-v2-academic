package com.msa4lmsv2academic.domain.provisioning.service;

import com.msa4lmsv2academic.global.error.InvalidAdmissionCandidateRequestException;
import java.util.Locale;

/** 연도 2자리 + 학과 ID 2자리 + 학생/교수 ID 4자리. */
public final class AcademicNumberGenerator {
    private AcademicNumberGenerator() {}

    public static String departmentCode(Long departmentId) {
        if (departmentId == null || departmentId < 1 || departmentId > 99) {
            throw new InvalidAdmissionCandidateRequestException("학번·교번 발급에는 1~99 범위의 학과 ID가 필요합니다.");
        }
        return String.format(Locale.ROOT, "%02d", departmentId);
    }

    public static String generate(Short year, Long departmentId, Long entityId) {
        if (year == null || year < 1900 || entityId == null || entityId < 1 || entityId > 9999) {
            throw new InvalidAdmissionCandidateRequestException("연도는 1900년 이상, 학생/교수 ID는 1~9999여야 8자리 번호를 발급할 수 있습니다.");
        }
        return String.format(Locale.ROOT, "%02d%s%04d", year % 100, departmentCode(departmentId), entityId);
    }
}
