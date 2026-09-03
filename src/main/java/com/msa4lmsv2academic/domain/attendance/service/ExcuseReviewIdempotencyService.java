package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.request.ExcuseReviewRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.global.error.ExcuseReviewConflictException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKey;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import com.msa4lmsv2academic.global.idempotency.IdempotencyStatus;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import java.nio.charset.StandardCharsets;
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
@Transactional(readOnly = true, propagation = Propagation.MANDATORY)
public class ExcuseReviewIdempotencyService {

    public static final String ENDPOINT = "PATCH /api/academic/attendance/excuses/{requestId}";

    private final AcademicIdempotencyKeyRepository keyRepository;
    private final ObjectMapper objectMapper;

    public String hash(Long requestId, ExcuseReviewRequestDTO request) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(new ReviewFingerprint(requestId, request))
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<GlobalResponseDTO<ExcuseRequestResponseDTO>> replay(
            String key,
            Long userId,
            String hash,
            LocalDateTime now
    ) {
        AcademicIdempotencyKey saved = keyRepository.findByIdempotencyKey(key).orElse(null);
        if (saved == null) {
            return Optional.empty();
        }
        if (ENDPOINT.equals(saved.getEndpoint())
                && saved.getStatus() == IdempotencyStatus.COMPLETED
                && !saved.getExpiresAt().isAfter(now)) {
            keyRepository.deleteExpiredCompletedKey(key, ENDPOINT, now);
            return Optional.empty();
        }
        if (!saved.matches(userId, ENDPOINT, hash)
                || saved.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new ExcuseReviewConflictException("다른 요청에 사용했거나 처리 중인 멱등성 키입니다.");
        }
        return Optional.of(objectMapper.readValue(
                saved.getResponseSnapshot(),
                objectMapper.getTypeFactory().constructParametricType(
                        GlobalResponseDTO.class,
                        ExcuseRequestResponseDTO.class
                )
        ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AcademicIdempotencyKey reserve(
            String key,
            Long userId,
            String hash,
            LocalDateTime now
    ) {
        try {
            return keyRepository.saveAndFlush(AcademicIdempotencyKey.create(
                    key,
                    userId,
                    ENDPOINT,
                    hash,
                    now
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new ExcuseReviewConflictException("이미 사용 중인 멱등성 키입니다.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            AcademicIdempotencyKey key,
            GlobalResponseDTO<ExcuseRequestResponseDTO> response
    ) {
        key.complete(objectMapper.writeValueAsString(response));
        keyRepository.flush();
    }

    private record ReviewFingerprint(Long requestId, ExcuseReviewRequestDTO request) {
    }
}
