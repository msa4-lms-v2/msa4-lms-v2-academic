package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.NoticeCategory;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import java.time.LocalDate;
import java.util.Set;

public record NoticeSearchCondition(
        long offset,
        int limit,
        String keyword,
        String authorKeyword,
        NoticeCategory category,
        Set<NoticeTargetRole> targetRoles,
        Boolean active,
        LocalDate createdFrom,
        LocalDate createdTo
) {
}
