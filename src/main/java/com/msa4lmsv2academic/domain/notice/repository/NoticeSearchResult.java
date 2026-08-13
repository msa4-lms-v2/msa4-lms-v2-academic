package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import java.util.List;

public record NoticeSearchResult(List<Notice> items, long totalCount) {
}
