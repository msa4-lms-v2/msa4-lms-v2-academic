package com.msa4lmsv2academic.global.idempotency;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicIdempotencyKeyRepository extends JpaRepository<AcademicIdempotencyKey, Long> {
    Optional<AcademicIdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("delete from AcademicIdempotencyKey k where k.idempotencyKey = :key and k.endpoint = :endpoint "
            + "and k.status = com.msa4lmsv2academic.global.idempotency.IdempotencyStatus.COMPLETED "
            + "and k.expiresAt <= :now")
    int deleteExpiredCompletedKey(@Param("key") String key, @Param("endpoint") String endpoint,
                                  @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from AcademicIdempotencyKey k where k.endpoint = :endpoint "
            + "and k.status = com.msa4lmsv2academic.global.idempotency.IdempotencyStatus.COMPLETED "
            + "and k.expiresAt <= :now")
    int deleteExpiredCompletedKeys(@Param("endpoint") String endpoint, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from AcademicIdempotencyKey k where k.endpoint like concat(:prefix, '%') "
            + "and k.status = com.msa4lmsv2academic.global.idempotency.IdempotencyStatus.COMPLETED "
            + "and k.expiresAt <= :now")
    int deleteExpiredCompletedKeysByEndpointPrefix(@Param("prefix") String prefix, @Param("now") LocalDateTime now);
}
