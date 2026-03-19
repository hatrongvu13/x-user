package com.htv.xuser.config.security;

import com.htv.xuser.exception.ErrorCode;
import com.htv.xuser.filter.JwtAuthFilter;
import com.htv.xuser.model.response.ApiResponse;
import com.htv.xuser.services.msg.MessageService;
import com.htv.xuser.services.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * WebSecurityConfig — cấu hình bảo mật REST API stateless
 *
 * Token validation: Nimbus JOSE+JWT 10.7 qua {@link JwtAuthFilter}
 * Session: STATELESS (không dùng cookie session)
 * CSRF: disabled (Bearer token tự bảo vệ)
 *
 * ── Endpoint Access Map ─────────────────────────────────────────────────────
 *
 * [PUBLIC — không cần token]
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/refresh-token
 *   POST /api/v1/auth/forgot-password
 *   POST /api/v1/auth/reset-password
 *   GET  /api/v1/auth/reset-password/validate
 *   POST /api/v1/auth/verify-email
 *   POST /api/v1/auth/verify-email/resend
 *   POST /api/v1/auth/mfa/verify           ← dùng mfaPendingToken
 *   POST /api/v1/auth/mfa/resend-otp
 *   POST /api/v1/auth/mfa/backup-code      ← dùng mfaPendingToken
 *   GET  /actuator/health
 *   GET  /actuator/info
 *
 * [AUTHENTICATED — cần access token hợp lệ]
 *   POST /api/v1/auth/logout
 *   POST /api/v1/auth/mfa/enable
 *   POST /api/v1/auth/mfa/enable/confirm
 *   POST /api/v1/auth/mfa/disable
 *   POST /api/v1/auth/mfa/backup-codes/regenerate
 *   GET  /api/v1/users/me
 *   PUT  /api/v1/users/me
 *   POST /api/v1/users/me/change-password
 *
 * [USER:READ]
 *   GET /api/v1/users
 *   GET /api/v1/users/{id}
 *
 * [USER:WRITE]
 *   POST   /api/v1/users
 *   PUT    /api/v1/users/{id}
 *   PATCH  /api/v1/users/{id}/status
 *
 * [USER:DELETE]
 *   DELETE /api/v1/users/{id}
 *
 * [ROLE:READ]
 *   GET /api/v1/roles
 *   GET /api/v1/roles/{id}
 *
 * [ROLE:WRITE]
 *   POST   /api/v1/roles
 *   PUT    /api/v1/roles/{id}
 *   POST   /api/v1/users/{id}/roles
 *   DELETE /api/v1/users/{id}/roles/{rid}
 *
 * [ROLE:DELETE]
 *   DELETE /api/v1/roles/{id}
 *
 * [PERMISSION:READ]
 *   GET /api/v1/permissions
 *   GET /api/v1/permissions/{id}
 *
 * [PERMISSION:WRITE]
 *   POST   /api/v1/permissions
 *   PUT    /api/v1/permissions/{id}
 *   DELETE /api/v1/permissions/{id}
 *   POST   /api/v1/roles/{id}/permissions
 *   DELETE /api/v1/roles/{id}/permissions/{pid}
 *
 * [ROLE_ADMIN]
 *   /actuator/**  (trừ health và info đã public)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // bật @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ── Stateless REST — không session, không CSRF ─────────────────
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules ────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // ── Actuator ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // ── Auth — Public (không cần token) ───────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/verify-email/resend",
                                "/api/v1/auth/mfa/verify",
                                "/api/v1/auth/mfa/resend-otp",
                                "/api/v1/auth/mfa/backup-code"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/reset-password/validate").permitAll()

                        // ── Auth — Authenticated (cần token) ──────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/logout",
                                "/api/v1/auth/mfa/enable",
                                "/api/v1/auth/mfa/enable/confirm",
                                "/api/v1/auth/mfa/disable",
                                "/api/v1/auth/mfa/backup-codes/regenerate"
                        ).authenticated()

                        // ── Profile của chính mình ────────────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/change-password").authenticated()

                        // ── Users CRUD ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{id}").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/{id}").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/status").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasAuthority("USER:DELETE")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/{id}/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}/roles/{rid}").hasAuthority("ROLE:WRITE")

                        // ── Roles ─────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles/{id}").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/roles/{id}").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}").hasAuthority("ROLE:DELETE")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/roles/{id}/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/roles/{id}/permissions/{pid}").hasAuthority("PERMISSION:WRITE")

                        // ── Permissions ───────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")

                        // Tất cả request còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )

                // ── Exception Handling — trả JSON thay vì redirect ────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeJson(res, 401, ErrorCode.TOKEN_INVALID))
                        .accessDeniedHandler((req, res, e) ->
                                writeJson(res, 403, ErrorCode.ACCESS_DENIED))
                )

                // ── JWT Filter — trước UsernamePasswordAuthenticationFilter ───
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    // =========================================================================
    // AUTHENTICATION MANAGER
    // =========================================================================

    @Bean
    public AuthenticationManager authenticationManager() {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    // =========================================================================
    // PASSWORD ENCODER
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 12 ≈ 250ms/hash — chống brute-force
        return new BCryptPasswordEncoder(12);
    }


    // =========================================================================
    // HELPERS
    // =========================================================================

    private void writeJson(jakarta.servlet.http.HttpServletResponse res,
                           int status, ErrorCode code) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        String message = messageService.get(code.getMessageKey());
        res.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(code, message))
        );
    }
}