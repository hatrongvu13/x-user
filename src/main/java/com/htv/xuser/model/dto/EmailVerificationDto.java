package com.htv.xuser.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * EmailVerificationDto — Request/Response cho xác thực email
 * <p>
 * Tính năng:
 * [1] Xác thực email bằng token (GET qua link trong mail)
 * [2] Gửi lại email xác thực
 */
public final class EmailVerificationDto {

    private EmailVerificationDto() {
    }

    // =========================================================================
    // [1] XÁC THỰC EMAIL
    // =========================================================================

    /**
     * POST /api/v1/auth/verify-email
     * (hoặc GET /api/v1/auth/verify-email?token=... nếu click từ mail)
     */
    @Getter
    @Setter
    public static class VerifyRequest {

        @NotBlank(message = "{validation.required}")
        private String token;
    }

    @Getter
    @Builder
    public static class VerifyResponse {
        private boolean emailVerified;
        private String email;
        private Instant verifiedAt;
        private String message;
    }

    // =========================================================================
    // [2] GỬI LẠI EMAIL XÁC THỰC
    // =========================================================================

    /**
     * POST /api/v1/auth/verify-email/resend
     */
    @Getter
    @Setter
    public static class ResendRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        private String email;
    }

    @Getter
    @Builder
    public static class ResendResponse {
        private String maskedEmail;         // u***@gmail.com
        private String message;
        private int retryAfterSeconds;
    }
}
