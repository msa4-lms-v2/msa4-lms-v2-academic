package com.msa4lmsv2academic.domain.grade.service;

import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
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
public class GradeIdempotencyService {

    public static final String CREATE_ENDPOINT = "POST /api/academic/grades";
    public static final String UPDATE_ENDPOINT = "PATCH /api/academic/grades";
    public static final String FINALIZE_ENDPOINT = "PATCH /api/academic/grades/classes/{classId}/status";

    private final AcademicIdempotencyKeyRepository keyRepository;
    private final ObjectMapper objectMapper;

    public String hash(Object request) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<GlobalResponseDTO<GradeClassResponseDTO>> replay(
            String key, Long userId, String endpoint, String hash, LocalDateTime now
    ) {
        AcademicIdempotencyKey saved = keyRepository.findByIdempotencyKey(key).orElse(null);
        if (saved == null) {
            return Optional.empty();
        }
        if (endpoint.equals(saved.getEndpoint())
                && saved.getStatus() == IdempotencyStatus.COMPLETED
                && !saved.getExpiresAt().isAfter(now)) {
            keyRepository.deleteExpiredCompletedKey(key, endpoint, now);
            return Optional.empty();
        }
        if (!saved.matches(userId, endpoint, hash)
                || saved.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new GradeManagementConflictException("다른 요청에 사용했거나 처리 중인 멱등성 키입니다.");
        }
        return Optional.of(objectMapper.readValue(
                saved.getResponseSnapshot(),
                objectMapper.getTypeFactory().constructParametricType(
                        GlobalResponseDTO.class,
                        GradeClassResponseDTO.class
                )
        ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AcademicIdempotencyKey reserve(
            String key, Long userId, String endpoint, String hash, LocalDateTime now
    ) {
        try {
            return keyRepository.saveAndFlush(AcademicIdempotencyKey.create(
                    key, userId, endpoint, hash, now
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new GradeManagementConflictException("이미 사용 중인 멱등성 키입니다.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            AcademicIdempotencyKey key,
            GlobalResponseDTO<GradeClassResponseDTO> response
    ) {
        key.complete(objectMapper.writeValueAsString(response));
        keyRepository.flush();
    }
}
