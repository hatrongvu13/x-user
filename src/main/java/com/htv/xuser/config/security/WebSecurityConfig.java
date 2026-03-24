package com.htv.xuser.config.security;

import com.htv.xuser.exception.ErrorCode;
import com.htv.xuser.filter.JwtAuthFilter;
import com.htv.xuser.model.response.ApiResponse;
import com.htv.xuser.services.msg.MessageService;
import com.htv.xuser.services.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper           objectMapper;
    private final MessageService         messageService;

    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/verify-email/resend",
            "/api/v1/auth/mfa/verify",
            "/api/v1/auth/mfa/resend-otp",
            "/api/v1/auth/mfa/backup-code",
    };

    private static final String[] PUBLIC_GET = {
            "/api/v1/auth/reset-password/validate",
            "/actuator/health",
            "/actuator/info",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info(">>> WebSecurityConfig LOADED <<<");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // PUBLIC trước — quan trọng nhất
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET,  PUBLIC_GET).permitAll()

                        // Actuator
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Auth cần token
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/logout",
                                "/api/v1/auth/mfa/enable",
                                "/api/v1/auth/mfa/enable/confirm",
                                "/api/v1/auth/mfa/disable",
                                "/api/v1/auth/mfa/backup-codes/regenerate"
                        ).authenticated()

                        // Profile
                        .requestMatchers(HttpMethod.GET,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/change-password").authenticated()

                        // Users
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{id}").hasAuthority("USER:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/{id}").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/status").hasAuthority("USER:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasAuthority("USER:DELETE")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/{id}/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}/roles/{rid}").hasAuthority("ROLE:WRITE")

                        // Roles
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/roles/{id}").hasAuthority("ROLE:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/roles").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/roles/{id}").hasAuthority("ROLE:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/{id}").hasAuthority("ROLE:DELETE")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/roles/{id}/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/roles/{id}/permissions/{pid}").hasAuthority("PERMISSION:WRITE")

                        // Permissions
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:READ")
                        .requestMatchers(HttpMethod.POST,   "/api/v1/permissions").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/permissions/{id}").hasAuthority("PERMISSION:WRITE")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeJson(res, 401, ErrorCode.TOKEN_INVALID))
                        .accessDeniedHandler((req, res, e) ->
                                writeJson(res, 403, ErrorCode.ACCESS_DENIED))
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse res,
                           int status, ErrorCode code) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        String message;
        try {
            message = messageService.get(code.getMessageKey());
        } catch (Exception ex) {
            message = code.name();
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(ApiResponse.error(code, message));
        } catch (Exception ex) {
            json = "{\"success\":false,\"code\":%d,\"message\":\"%s\"}"
                    .formatted(code.getCode(), message);
        }
        res.getWriter().write(json);
    }
}