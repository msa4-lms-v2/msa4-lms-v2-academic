package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import java.util.Set;

public record NoticeSearchCondition(
        long offset,
        int limit,
        String keyword,
        Set<NoticeTargetRole> targetRoles,
        Boolean active
) {
}
