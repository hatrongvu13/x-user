package com.htv.xuser.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * AuthDto — Request/Response cho nhóm tính năng xác thực
 *
 * Tính năng bao gồm:
 *  [1] Đăng ký tài khoản
 *  [2] Đăng nhập (email + password)
 *  [3] Refresh token
 *  [4] Đăng xuất
 */
public final class AuthDto {
    private AuthDto() {}

    // =========================================================================
    // [1] ĐĂNG KÝ
    // =========================================================================

    /**
     * POST /api/v1/auth/register
     */
    @Getter
    @Setter
    public static class RegisterRequest {

        @NotBlank(message = "{user.email.field}")
        @Email(message = "{validation.invalid.format}")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "{user.username.field}")
        @Size(min = 3, max = 50, message = "{validation.size.range}")
        @Pattern(
                regexp  = "^[a-zA-Z0-9._-]+$",
                message = "{validation.invalid.format}"
        )
        private String username;

        @NotBlank(message = "{user.password.field}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String password;

        @NotBlank(message = "{user.password.field}")
        @Size(min = 8, max = 100)
        private String confirmPassword;   // validate equals(password) ở service

        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Pattern(
                regexp  = "^\\+?[0-9]{9,15}$",
                message = "{validation.invalid.format}"
        )
        private String phoneNumber;
    }

    @Getter
    @Builder
    public static class RegisterResponse {
        private UUID    userId;
        private String  email;
        private String  username;
        private boolean emailVerified;    // luôn false ngay sau đăng ký
        private String  message;          // "Vui lòng kiểm tra email để xác thực tài khoản"
    }

    // =========================================================================
    // [2] ĐĂNG NHẬP
    // =========================================================================

    /**
     * POST /api/v1/auth/login
     *
     * Nếu tài khoản đã bật MFA:
     *   - Response trả về mfaRequired=true, mfaPendingToken (không có accessToken)
     *   - Client submit tiếp qua POST /api/v1/auth/mfa/verify
     */
    @Getter
    @Setter
    public static class LoginRequest {

        @NotBlank(message = "{user.email.field}")
        @Email(message = "{validation.invalid.format}")
        private String email;

        @NotBlank(message = "{user.password.field}")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginResponse {

        // Token — chỉ có khi mfaRequired = false
        private String  accessToken;
        private String  refreshToken;
        private String  tokenType;              // "Bearer"
        private Instant accessTokenExpiresAt;
        private Instant refreshTokenExpiresAt;

        // MFA pending — chỉ có khi mfaRequired = true
        private boolean mfaRequired;
        private String  mfaType;               // "TOTP" | "EMAIL_OTP"

        /**
         * Token ngắn hạn (5 phút) dùng để submit mã MFA.
         * Chỉ có khi mfaRequired = true.
         * Không thể dùng để gọi API thông thường.
         */
        private String  mfaPendingToken;

        // Thông tin user — có trong cả 2 trường hợp
        private UserInfo user;
    }

    // =========================================================================
    // [3] REFRESH TOKEN
    // =========================================================================

    /**
     * POST /api/v1/auth/refresh-token
     */
    @Getter
    @Setter
    public static class RefreshTokenRequest {

        @NotBlank(message = "{validation.required}")
        private String refreshToken;
    }

    @Getter
    @Builder
    public static class RefreshTokenResponse {
        private String  accessToken;
        private String  refreshToken;        // rotation: trả về refresh token mới
        private String  tokenType;
        private Instant accessTokenExpiresAt;
        private Instant refreshTokenExpiresAt;
    }

    // =========================================================================
    // [4] ĐĂNG XUẤT
    // =========================================================================

    /**
     * POST /api/v1/auth/logout
     */
    @Getter
    @Setter
    public static class LogoutRequest {

        @NotBlank(message = "{validation.required}")
        private String refreshToken;

        /**
         * true = thu hồi tất cả refresh token của user (đăng xuất tất cả thiết bị).
         * false (default) = chỉ thu hồi refreshToken trong request này.
         */
        private boolean logoutAllDevices = false;
    }

    // =========================================================================
    // INNER — UserInfo (nhúng vào LoginResponse)
    // =========================================================================

    @Getter
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String      email;
        private String      username;
        private String      firstName;
        private String      lastName;
        private String      avatarUrl;
        private boolean     emailVerified;
        private boolean     mfaEnabled;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
