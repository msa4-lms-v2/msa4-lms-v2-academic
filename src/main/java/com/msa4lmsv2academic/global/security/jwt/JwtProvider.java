package com.msa4lmsv2academic.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Set;

@Slf4j
@Component
public class JwtProvider {

    private static final Set<String> ALLOWED_ROLES = Set.of("STUDENT", "PROFESSOR", "ADMIN");

    private final SecretKey secretKey;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Long.valueOf(claims.getSubject());
            return ALLOWED_ROLES.contains(claims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("JWT 검증 실패: {}", exception.getMessage());
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
