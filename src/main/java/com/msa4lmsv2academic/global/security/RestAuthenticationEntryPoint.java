package com.msa4lmsv2academic.global.security;

import com.msa4lmsv2academic.global.response.CustomResponseCode;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.filter.GatewayHeaderAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        boolean invalidAuthentication = Boolean.TRUE.equals(
                request.getAttribute(GatewayHeaderAuthenticationFilter.INVALID_AUTHENTICATION_ATTRIBUTE));
        CustomResponseCode code = invalidAuthentication
                ? CustomResponseCode.INVALID_TOKEN
                : CustomResponseCode.UNAUTHENTICATED;
        response.setStatus(code.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(GlobalRes.fail(code, null)));
    }
}
