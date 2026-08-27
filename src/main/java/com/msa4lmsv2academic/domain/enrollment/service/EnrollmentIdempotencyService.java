package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentApplicationRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCreateResponseDTO;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
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
public class EnrollmentIdempotencyService {
    public static final String ENDPOINT = "POST /api/academic/enrollments";

    private final AcademicIdempotencyKeyRepository keyRepository;
    private final ObjectMapper objectMapper;

    public String hash(StudentEnrollmentCreateRequestDTO request) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    // 키만 별도 커밋되지 않도록 반드시 신청 서비스의 기존 쓰기 transaction에 참여합니다.
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<GlobalResponseDTO<StudentEnrollmentCreateResponseDTO>> replay(
            String key, Long userId, String hash, LocalDateTime now
    ) {
        AcademicIdempotencyKey saved = keyRepository.findByIdempotencyKey(key).orElse(null);
        if (saved == null) {
            return Optional.empty();
        }
        if (ENDPOINT.equals(saved.getEndpoint()) && saved.getStatus() == IdempotencyStatus.COMPLETED
                && !saved.getExpiresAt().isAfter(now)) {
            keyRepository.deleteExpiredCompletedKey(key, ENDPOINT, now);
            return Optional.empty();
        }
        if (!saved.matches(userId, ENDPOINT, hash) || saved.getStatus() != IdempotencyStatus.COMPLETED) {
            throw EnrollmentApplicationRejectedException.from(EnrollmentApplicationRejectionReason.IDEMPOTENCY_KEY_CONFLICT);
        }
        return Optional.of(objectMapper.readValue(saved.getResponseSnapshot(),
                objectMapper.getTypeFactory().constructParametricType(
                        GlobalResponseDTO.class, StudentEnrollmentCreateResponseDTO.class)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AcademicIdempotencyKey reserve(String key, Long userId, String hash, LocalDateTime now) {
        try {
            return keyRepository.saveAndFlush(AcademicIdempotencyKey.create(key, userId, ENDPOINT, hash, now));
        } catch (DataIntegrityViolationException exception) {
            // 다른 학생의 동시 요청도 전역 unique key를 공유합니다. 예약 충돌은 전체 신청을 롤백합니다.
            throw EnrollmentApplicationRejectedException.from(EnrollmentApplicationRejectionReason.IDEMPOTENCY_KEY_CONFLICT);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(AcademicIdempotencyKey key, GlobalResponseDTO<StudentEnrollmentCreateResponseDTO> response) {
        key.complete(objectMapper.writeValueAsString(response));
        keyRepository.flush();
    }
}
