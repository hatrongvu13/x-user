package com.htv.xuser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * SecurityProperties — type-safe binding từ app.security.*
 *
 * Prefix: x-user.security
 *
 * application.yml:
 * <pre>
 * x-user:
 *   security:
 *     max-failed-login-attempts: 5
 *     account-lock-duration-seconds: 900
 *     email-resend-cooldown-seconds: 60
 *     allowed-origins: "http://localhost:3000,http://localhost:5173"
 * </pre>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "x-user.security")
public class SecurityProperties {

    /** Số lần sai password tối đa trước khi lock. Default: 5 */
    private int maxFailedLoginAttempts = 5;

    /** Thời gian khóa tài khoản (giây). Default: 900 (15 phút) */
    private long accountLockDurationSeconds = 900;

    /** Cooldown giữa 2 lần gửi email (giây). Default: 60 */
    private long emailResendCooldownSeconds = 60;

    /** CORS origins, phân cách bằng dấu phẩy */
    private String allowedOrigins = "http://localhost:3000";
}