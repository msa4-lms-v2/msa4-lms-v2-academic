package com.msa4lmsv2academic.global.security.filter;

import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.global.security.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_TOKEN_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".invalidToken";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_SCHEME = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            if (jwtProvider.isValid(token)) {
                CurrentUser currentUser = new CurrentUser(jwtProvider.getUserId(token), jwtProvider.getRole(token));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + currentUser.role()));
                var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_SCHEME)) {
            return header.substring(BEARER_SCHEME.length());
        }
        return null;
    }
}
