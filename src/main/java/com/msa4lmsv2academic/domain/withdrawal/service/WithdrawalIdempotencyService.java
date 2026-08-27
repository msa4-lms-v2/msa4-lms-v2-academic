package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.global.error.InvalidWithdrawalRequestException;
import com.msa4lmsv2academic.global.error.WithdrawalIdempotencyConflictException;
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
@Transactional(propagation = Propagation.MANDATORY)
public class WithdrawalIdempotencyService {
    private final AcademicIdempotencyKeyRepository keyRepository;
    private final ObjectMapper objectMapper;

    public void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100 || key.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidWithdrawalRequestException("1~100자의 공백 없는 Idempotency-Key가 필요합니다.");
        }
    }

    public String hash(Object body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(body)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public Optional<WithdrawalResponseDTO> replay(String key, Long userId, String endpoint, String hash,
                                                  LocalDateTime now) {
        AcademicIdempotencyKey saved = keyRepository.findByIdempotencyKey(key).orElse(null);
        if (saved == null) {
            return Optional.empty();
        }
        if (endpoint.equals(saved.getEndpoint()) && saved.getStatus() == IdempotencyStatus.COMPLETED
                && !saved.getExpiresAt().isAfter(now)) {
            keyRepository.deleteExpiredCompletedKey(key, endpoint, now);
            return Optional.empty();
        }
        if (!saved.matches(userId, endpoint, hash) || saved.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new WithdrawalIdempotencyConflictException("이미 다른 요청에 사용되었거나 처리 중인 멱등성 키입니다.");
        }
        return Optional.of(objectMapper.readValue(saved.getResponseSnapshot(), WithdrawalResponseDTO.class));
    }

    public AcademicIdempotencyKey reserve(String key, Long userId, String endpoint, String hash,
                                          LocalDateTime now) {
        try {
            return keyRepository.saveAndFlush(AcademicIdempotencyKey.create(key, userId, endpoint, hash, now));
        } catch (DataIntegrityViolationException exception) {
            // 키 예약은 업무와 같은 transaction입니다. 실패하면 자퇴/감사와 함께 rollback합니다.
            if (isKeyDuplicate(exception)) {
                throw new WithdrawalIdempotencyConflictException("동일한 멱등성 키가 동시에 사용되었습니다.");
            }
            throw exception;
        }
    }

    public void complete(AcademicIdempotencyKey key, WithdrawalResponseDTO response) {
        key.complete(objectMapper.writeValueAsString(response));
        keyRepository.flush();
    }

    private boolean isKeyDuplicate(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062
                    && sql.getMessage().contains("uk_idempotency_keys_key")) {
                return true;
            }
        }
        return false;
    }
}

