package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.global.error.InvalidLeaveRequestException;
import com.msa4lmsv2academic.global.error.LeaveRequestConflictException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import com.msa4lmsv2academic.global.idempotency.IdempotencyStatus;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class LeaveIdempotencyService {
    private final AcademicIdempotencyKeyRepository repository;
    private final ObjectMapper mapper;

    public void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100 || key.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidLeaveRequestException("1~100자의 공백 없는 Idempotency-Key가 필요합니다.");
        }
    }

    public String hash(Object body) {
        return digest(mapper.writeValueAsBytes(body));
    }

    public String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public <T> Optional<T> replay(String key, Long userId, String endpoint, String hash,
                                  LocalDateTime now, Class<T> responseType) {
        var saved = repository.findByIdempotencyKey(key).orElse(null);
        if (saved == null) return Optional.empty();
        // 파일 업로드 전 읽기 전용 조회에서는 만료 키도 삭제하지 않습니다.
        if (endpoint.equals(saved.getEndpoint()) && saved.getStatus() == IdempotencyStatus.COMPLETED
                && !saved.getExpiresAt().isAfter(now)) return Optional.empty();
        if (!saved.matches(userId, endpoint, hash) || saved.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new LeaveRequestConflictException("다른 요청에 사용되었거나 처리 중인 멱등성 키입니다.");
        }
        return Optional.of(mapper.readValue(saved.getResponseSnapshot(), responseType));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AcademicIdempotencyKey reserve(String key, Long userId, String endpoint, String hash, LocalDateTime now) {
        repository.deleteExpiredCompletedKey(key, endpoint, now);
        try {
            return repository.saveAndFlush(AcademicIdempotencyKey.create(key, userId, endpoint, hash, now));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                        && sql.getMessage().contains("uk_idempotency_keys_key")) {
                    throw new LeaveRequestConflictException("동일한 멱등성 키가 동시에 사용되었습니다.");
                }
            }
            throw exception;
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(AcademicIdempotencyKey key, Object response) {
        key.complete(mapper.writeValueAsString(response));
        repository.flush();
    }
}
