package com.htv.xuser.config.security;

import com.htv.xuser.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * WebSecurityConfig — cấu hình bảo mật REST API
 *
 * Kiến trúc phân quyền 3 tầng:
 *   Tầng 1 — URL pattern (file này): endpoint nào cần quyền gì
 *   Tầng 2 — @PreAuthorize (Controller): fine-grained, VD: chỉ chính user đó
 *   Tầng 3 — Service layer: business rules, VD: không xóa role system
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ Endpoint map đầy đủ                                             │
 * ├──────────────────────────────────┬──────────────────────────────┤
 * │ POST /api/v1/auth/register        │ Public                       │
 * │ POST /api/v1/auth/login           │ Public                       │
 * │ POST /api/v1/auth/refresh-token   │ Public                       │
 * │ POST /api/v1/auth/logout          │ Authenticated                │
 * │ POST /api/v1/auth/forgot-password │ Public                       │
 * │ POST /api/v1/auth/reset-password  │ Public                       │
 * │ POST /api/v1/auth/verify-email    │ Public                       │
 * │ POST /api/v1/auth/verify-email/resend│ Public                   │
 * │ POST /api/v1/auth/mfa/verify      │ Public (dùng mfaPendingToken)│
 * │ POST /api/v1/auth/mfa/resend-otp  │ Public                       │
 * │ POST /api/v1/auth/mfa/enable      │ Authenticated                │
 * │ POST /api/v1/auth/mfa/enable/confirm│ Authenticated              │
 * │ POST /api/v1/auth/mfa/disable     │ Authenticated                │
 * │ POST /api/v1/auth/mfa/backup-code │ Public (dùng mfaPendingToken)│
 * │ POST /api/v1/auth/mfa/backup-codes/regenerate│ Authenticated    │
 * ├──────────────────────────────────┬──────────────────────────────┤
 * │ GET  /api/v1/users/me            │ Authenticated                │
 * │ PUT  /api/v1/users/me            │ Authenticated                │
 * │ POST /api/v1/users/me/change-password│ Authenticated            │
 * │ GET  /api/v1/users               │ USER:READ                    │
 * │ GET  /api/v1/users/{id}          │ USER:READ                    │
 * │ POST /api/v1/users               │ USER:WRITE                   │
 * │ PUT  /api/v1/users/{id}          │ USER:WRITE                   │
 * │ PATCH/api/v1/users/{id}/status   │ USER:WRITE                   │
 * │ DELETE /api/v1/users/{id}        │ USER:DELETE                  │
 * │ POST /api/v1/users/{id}/roles    │ ROLE:WRITE                   │
 * │ DELETE /api/v1/users/{id}/roles/{rid}│ ROLE:WRITE               │
 * ├──────────────────────────────────┬──────────────────────────────┤
 * │ GET  /api/v1/roles               │ ROLE:READ                    │
 * │ GET  /api/v1/roles/{id}          │ ROLE:READ                    │
 * │ POST /api/v1/roles               │ ROLE:WRITE                   │
 * │ PUT  /api/v1/roles/{id}          │ ROLE:WRITE                   │
 * │ DELETE /api/v1/roles/{id}        │ ROLE:DELETE                  │
 * │ POST /api/v1/roles/{id}/permissions│ PERMISSION:WRITE           │
 * │ DELETE /api/v1/roles/{id}/permissions/{pid}│ PERMISSION:WRITE   │
 * ├──────────────────────────────────┬──────────────────────────────┤
 * │ GET  /api/v1/permissions         │ PERMISSION:READ              │
 * │ GET  /api/v1/permissions/{id}    │ PERMISSION:READ              │
 * │ POST /api/v1/permissions         │ PERMISSION:WRITE             │
 * │ PUT  /api/v1/permissions/{id}    │ PERMISSION:WRITE             │
 * │ DELETE /api/v1/permissions/{id}  │ PERMISSION:WRITE             │
 * └──────────────────────────────────┴──────────────────────────────┘
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← bật @PreAuthorize trên từng method
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // ── Public — không cần token ──────────────────────────────────────────────
    private static final String[] PUBLIC_POST = {
            // Auth
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/verify-email/resend",
            // MFA — dùng mfaPendingToken (không phải access token)
            "/api/v1/auth/mfa/verify",
            "/api/v1/auth/mfa/resend-otp",
            "/api/v1/auth/mfa/backup-code",
    };

    private static final String[] PUBLIC_GET = {
            // Password reset form validate token trước khi render
            "/api/v1/auth/reset-password/validate",
            // Actuator
            "/actuator/health",
            "/actuator/info",
    };

    // =========================================================================
    //  SECURITY FILTER CHAIN
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ── Tắt CSRF (stateless REST) ────────────────────────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Stateless — không dùng session ───────────────────────────────
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization ─────────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // ── Actuator ──────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // ── Auth endpoints ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()

                        // Logout — cần access token để revoke đúng
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                        // MFA management — cần đang đăng nhập
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/mfa/enable",
                                "/api/v1/auth/mfa/enable/confirm",
                                "/api/v1/auth/mfa/disable",
                                "/api/v1/auth/mfa/backup-codes/regenerate"
                        ).authenticated()

                        // ── Users API ─────────────────────────────────────────────────
                        // Profile của chính mình
                        .requestMatchers(HttpMethod.GET,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/change-password").authenticated()

                        // CRUD user — cần permission
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{id}").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/{id}").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/status").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasAuthority("USER:DELETE")

                        // Role assignment
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/{id}/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}/roles/{rid}").hasAuthority("ROLE:WRITE")

                        // ── Roles API ──────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles/{id}").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/roles/{id}").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}").hasAuthority("ROLE:DELETE")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles/{id}/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}/permissions/{pid}").hasAuthority("PERMISSION:WRITE")

                        // ── Permissions API ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")

                        // ── Tất cả request còn lại phải đăng nhập ─────────────────────
                        .anyRequest().authenticated()
                )

                // ── Exception handling — JSON response (không redirect) ───────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("""
                        {"success":false,"message":"Bạn cần đăng nhập để thực hiện thao tác này"}
                        """);
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("""
                        {"success":false,"message":"Bạn không có quyền thực hiện thao tác này"}
                        """);
                        })
                )

                // ── JWT filter — chạy trước UsernamePasswordAuthenticationFilter ──
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
