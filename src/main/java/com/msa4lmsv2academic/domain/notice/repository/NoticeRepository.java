package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
