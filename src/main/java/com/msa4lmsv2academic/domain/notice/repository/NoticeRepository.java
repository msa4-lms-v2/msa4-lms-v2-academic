package com.msa4lmsv2academic.domain.notice.repository;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeCategory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByCategoryAndNormalTransitionDateLessThanEqual(
            NoticeCategory category, LocalDate normalTransitionDate
    );
}
