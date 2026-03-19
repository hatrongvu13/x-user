package com.htv.xuser.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * PasswordDto — Request/Response cho quản lý mật khẩu
 *
 * Tính năng:
 *  [1] Quên mật khẩu — gửi email reset
 *  [2] Đặt lại mật khẩu bằng token từ email
 *  [3] Đổi mật khẩu khi đang đăng nhập
 *  [4] Validate token reset (kiểm tra hợp lệ trước khi render form)
 */
public final class PasswordDto {

    private PasswordDto() {}

    // =========================================================================
    // [1] QUÊN MẬT KHẨU
    // =========================================================================

    /**
     * POST /api/v1/auth/forgot-password
     */
    @Getter
    @Setter
    public static class ForgotPasswordRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        private String email;
    }

    @Getter
    @Builder
    public static class ForgotPasswordResponse {
        private String  maskedEmail;        // u***@gmail.com
        private String  message;
        private int     retryAfterSeconds;  // chống spam — thời gian chờ trước lần gửi tiếp
    }

    // =========================================================================
    // [2] ĐẶT LẠI MẬT KHẨU
    // =========================================================================

    /**
     * POST /api/v1/auth/reset-password
     */
    @Getter
    @Setter
    public static class ResetPasswordRequest {

        @NotBlank(message = "{validation.required}")
        private String token;              // UUID từ link trong email

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String newPassword;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String confirmPassword;    // validate equals(newPassword) ở service
    }

    @Getter
    @Builder
    public static class ResetPasswordResponse {
        private String  message;
    }

    // =========================================================================
    // [3] ĐỔI MẬT KHẨU (KHI ĐANG ĐĂNG NHẬP)
    // =========================================================================

    /**
     * POST /api/v1/users/me/change-password
     */
    @Getter
    @Setter
    public static class ChangePasswordRequest {

        @NotBlank(message = "{validation.required}")
        private String currentPassword;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String newPassword;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String confirmPassword;
    }

    // =========================================================================
    // [4] VALIDATE TOKEN RESET
    // =========================================================================

    /**
     * GET /api/v1/auth/reset-password/validate?token=...
     *
     * Frontend gọi trước khi render form đặt lại mật khẩu
     * để tránh hiển thị form khi token đã hết hạn.
     */
    @Getter
    @Builder
    public static class ValidateTokenResponse {
        private boolean valid;
        private String  maskedEmail;    // hiển thị email đang reset (đã che)
        private String  message;
    }
}