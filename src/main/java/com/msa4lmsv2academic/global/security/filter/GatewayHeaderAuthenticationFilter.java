package com.msa4lmsv2academic.global.security.filter;

import com.msa4lmsv2academic.global.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_AUTHENTICATION_ATTRIBUTE =
            GatewayHeaderAuthenticationFilter.class.getName() + ".invalidAuthentication";

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final Set<String> ALLOWED_ROLES = Set.of("STUDENT", "PROFESSOR", "ADMIN");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String roleHeader = request.getHeader(USER_ROLE_HEADER);

        if (userIdHeader == null && roleHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = Long.valueOf(userIdHeader);
            if (userId <= 0 || !ALLOWED_ROLES.contains(roleHeader)) {
                throw new IllegalArgumentException("Invalid gateway authentication headers");
            }

            CurrentUser currentUser = new CurrentUser(userId, roleHeader);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleHeader));
            var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException | NullPointerException exception) {
            request.setAttribute(INVALID_AUTHENTICATION_ATTRIBUTE, Boolean.TRUE);
        }

        filterChain.doFilter(request, response);
    }
}
