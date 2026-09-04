package com.msa4lmsv2academic.global.security;

import com.msa4lmsv2academic.global.security.filter.GatewayHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**"
    };

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean localProfile = environment.acceptsProfiles(Profiles.of("local"));

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (localProfile) {
                        auth.requestMatchers(
                                HttpMethod.POST,
                                "/api/academic/account-provisionings/students",
                                "/api/academic/account-provisionings/professors"
                        ).permitAll();
                    }

                    auth.requestMatchers(SWAGGER_PATHS).permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    // Kafka 24시간 보관을 넘긴 gap 백필용 시스템 요청 - X-User-Id/X-User-Role 없이 온다.
                    // 이 Pod가 외부에 직접 노출되지 않는다는 전제(NetworkPolicy)로만 접근을 제한한다.
                    auth.requestMatchers(
                            HttpMethod.GET,
                            "/api/academic/students/*/snapshot",
                            "/api/academic/students/by-user/*/snapshot",
                            "/api/academic/catalog/semesters/*/snapshot",
                            "/api/academic/withdrawals/*/snapshot"
                    ).permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'"
                        ))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                        ))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy",
                                "camera=(), microphone=(), geolocation=()"
                        )))
                .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
