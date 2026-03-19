package com.htv.xuser.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JwtProperties — type-safe binding từ application.yml
 *
 * Prefix: x-user.jwt
 *
 * application.yml:
 * <pre>
 * x-user:
 *   jwt:
 *     secret: ${JWT_SECRET}               # min 64 ký tự để đủ cho HS512
 *     access-ttl-seconds: 900             # 15 phút
 *     refresh-ttl-seconds: 604800         # 7 ngày
 *     mfa-pending-ttl-seconds: 300        # 5 phút
 *     password-reset-ttl-seconds: 3600    # 1 giờ
 *     email-verify-ttl-seconds: 86400     # 24 giờ
 *     issuer: x-user-service
 * </pre>
 *
 * Token signing: HMAC-SHA512 via Nimbus JOSE+JWT 10.7
 * Không dùng JJWT (CVE bảo mật mới).
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "x-user.jwt")
public class JwtProperties {

    /**
     * HMAC-SHA512 signing secret.
     * Yêu cầu: min 64 ký tự (512 bits) để dùng HS512.
     * Production: inject từ ENV var hoặc Vault.
     */
    @NotBlank
    @Size(min = 64, message = "JWT secret phải có ít nhất 64 ký tự để dùng HS512")
    private String secret;

    /**
     * Access token TTL tính bằng giây.
     * Default: 900 (15 phút).
     */
    @Min(60)
    private long accessTtlSeconds = 900;

    /**
     * Refresh token TTL tính bằng giây.
     * Default: 604800 (7 ngày).
     */
    @Min(300)
    private long refreshTtlSeconds = 604_800;

    /**
     * MFA pending token TTL tính bằng giây.
     * Token tạm thời sau bước đăng nhập đúng password nhưng chưa qua MFA.
     * Default: 300 (5 phút).
     */
    @Min(60)
    private long mfaPendingTtlSeconds = 300;

    /**
     * Password reset token TTL tính bằng giây.
     * Default: 3600 (1 giờ).
     */
    @Min(300)
    private long passwordResetTtlSeconds = 3_600;

    /**
     * Email verify token TTL tính bằng giây.
     * Default: 86400 (24 giờ).
     */
    @Min(600)
    private long emailVerifyTtlSeconds = 86_400;

    /**
     * Issuer claim (iss) trong JWT.
     */
    @NotBlank
    private String issuer = "x-user-service";
}