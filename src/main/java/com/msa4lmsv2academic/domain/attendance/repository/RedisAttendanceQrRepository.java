package com.msa4lmsv2academic.domain.attendance.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisAttendanceQrRepository {

    private static final String KEY_PREFIX =
            "academic:attendance:qr:";

    private final StringRedisTemplate redisTemplate;

    /**
     * QR을 Redis에 활성 상태로 저장합니다.
     *
     * Key:
     * academic:attendance:qr:{sessionId}:{jti}
     *
     * Value:
     * active
     *
     * TTL:
     * QR 유효시간
     */
    public void save(
            long sessionId,
            String tokenHash,
            Duration ttl
    ) {
        redisTemplate.opsForValue().set(
                key(sessionId, tokenHash),
                "active",
                ttl
        );
    }

    /**
     * 스캔된 QR이 현재 유효한 QR인지 확인합니다.
     */
    public boolean exists(long sessionId, String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key(sessionId, jti))
        );
    }

    /**
     * 특정 QR을 즉시 무효화합니다.
     */
    public void delete(long sessionId, String jti) {
        redisTemplate.delete(key(sessionId, jti));
    }

    private String key(long sessionId, String jti) {
        return KEY_PREFIX + sessionId + ":" + jti;
    }
}