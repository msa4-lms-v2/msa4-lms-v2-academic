package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.NoticeAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    List<NoticeAttachment> findByNoticeIdOrderByIdAsc(Long noticeId);

    long countByNoticeId(Long noticeId);
}
