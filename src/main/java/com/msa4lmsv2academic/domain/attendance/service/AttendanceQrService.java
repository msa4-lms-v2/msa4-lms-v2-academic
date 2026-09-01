package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.repository.RedisAttendanceQrRepository;
import com.msa4lmsv2academic.domain.attendance.response.AttendanceQrResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
// 랜덤 토큰 생성, hash변환, redis저장, url생성
public class AttendanceQrService {

    // 발급된 QR 토큰이 Redis에서 유효한 시간
    private static final Duration QR_TTL = Duration.ofSeconds(20);

    // 화면에서 QR을 10초마다 새로 발급/갱신하기 위한 값
    private static final int REFRESH_SECONDS = 10;
    private final RedisAttendanceQrRepository redisAttendanceQrRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${attendance.qr.base-url}")
    private String qrBaseUrl;

    // 출석 세션에 사용할 QR 발급
    public AttendanceQrResponseDTO issue(Long sessionId) {
        String rawToken = generateToken(); // 랜덤 토큰 생성
        String tokenHash = hash(rawToken); // SHA-256 해시

        redisAttendanceQrRepository.save(
                sessionId,
                tokenHash,
                QR_TTL
        );

        String qrUrl = UriComponentsBuilder
                .fromUriString(qrBaseUrl)
                .queryParam("sessionId", sessionId)
                .queryParam("token", rawToken) // QR Url에는 원본 rawToken
                .build()
                .toUriString();

        return new AttendanceQrResponseDTO(
                qrUrl,
                LocalDateTime.now().plus(QR_TTL),
                REFRESH_SECONDS
        );
        // 학생이 QR을 찍어서 서버에 rawToken을 보내면 서버가 똑같이 SHA-256 해시해서 Redis 값과 비교
    }

    // 토큰의 랜덤값을 만듦
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }

    // QR 검증 메서드
    public boolean isValid(
            Long sessionId,
            String rawToken
    ) {
        String tokenHash = hash(rawToken);

        return redisAttendanceQrRepository.exists(
                sessionId,
                tokenHash
        );
    }
}
