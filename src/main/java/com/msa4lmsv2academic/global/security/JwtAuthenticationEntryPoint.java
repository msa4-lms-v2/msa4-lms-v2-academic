package com.msa4lmsv2academic.global.security;

import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        boolean invalidToken = Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE));
        CustomResponseCode code = invalidToken
                ? CustomResponseCode.INVALID_TOKEN
                : CustomResponseCode.UNAUTHENTICATED;
        String message = invalidToken ? "유효하지 않은 토큰입니다." : "인증이 필요합니다.";

        response.setStatus(code.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(GlobalRes.fail(code, message, null)));
    }
}
