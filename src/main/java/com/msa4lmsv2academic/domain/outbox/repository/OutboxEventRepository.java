package com.msa4lmsv2academic.domain.outbox.repository;

import com.msa4lmsv2academic.domain.outbox.entity.OutboxEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT * FROM outbox_events "
            + "WHERE status = 'PENDING' AND next_attempt_at <= :now "
            + "ORDER BY id ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("now") LocalDateTime now, @Param("batchSize") int batchSize);
}
