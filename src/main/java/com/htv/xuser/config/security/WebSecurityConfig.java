package com.htv.xuser.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WebSecurityConfig
 *
 * Phân quyền theo 3 tầng:
 *   Tầng 1 — URL pattern (file này):   ai được gọi endpoint nào
 *   Tầng 2 — @PreAuthorize (controller): fine-grained, VD: chỉ chính user đó
 *   Tầng 3 — Service layer:             business rule, VD: không xóa role system
 *
 * Quy ước permission:
 *   hasAuthority('USER:READ')    — kiểm tra permission cụ thể
 *   hasRole('ADMIN')             — Spring tự thêm prefix ROLE_, tức ROLE_ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← bật @PreAuthorize trên từng method
public class WebSecurityConfig {

    // ── Public — không cần token ──────────────────────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/actuator/health",
            "/actuator/info",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── 1. Public ─────────────────────────────────────────────────
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // ── 2. Actuator — chỉ ADMIN ───────────────────────────────────
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // ── 3. Users API ──────────────────────────────────────────────
                        // Xem danh sách tất cả users — cần quyền USER:READ
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users").hasAuthority("USER:READ")
                        // Xem chi tiết 1 user — cần USER:READ (fine-grained check thêm ở @PreAuthorize)
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{id}").hasAuthority("USER:READ")
                        // Xem profile của chính mình — chỉ cần đăng nhập
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/me").authenticated()
                        // Cập nhật profile của chính mình — chỉ cần đăng nhập
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/me").authenticated()
                        // Tạo user mới — cần USER:WRITE
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users").hasAuthority("USER:WRITE")
                        // Cập nhật user bất kỳ — cần USER:WRITE
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/{id}").hasAuthority("USER:WRITE")
                        // Xóa user — cần USER:DELETE
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasAuthority("USER:DELETE")
                        // Đổi trạng thái user — cần USER:WRITE
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/status").hasAuthority("USER:WRITE")
                        // Gán / gỡ role cho user — cần ROLE:WRITE
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/{id}/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}/roles/{roleId}").hasAuthority("ROLE:WRITE")

                        // ── 4. Roles API ──────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles/**").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/roles/{id}").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}").hasAuthority("ROLE:DELETE")
                        // Gán / gỡ permission cho role — cần PERMISSION:WRITE
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles/{id}/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}/permissions/{pId}").hasAuthority("PERMISSION:WRITE")

                        // ── 5. Permissions API ────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions/**").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")

                        // ── 6. Mọi request còn lại phải đăng nhập ────────────────────
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
