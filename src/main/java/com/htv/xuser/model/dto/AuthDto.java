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
    // REGISTER
    // =========================================================================

    @Getter @Setter
    public static class RegisterRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "{validation.required}")
        @Size(min = 3, max = 50, message = "{validation.size.range}")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "{validation.invalid.format}")
        private String username;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String password;

        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "{validation.invalid.format}")
        private String phoneNumber;
    }

    @Getter @Builder
    public static class RegisterResponse {
        private UUID    userId;
        private String  email;
        private String  username;
        private boolean emailVerified;
        private String  message;
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    @Getter @Setter
    public static class LoginRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        private String email;

        @NotBlank(message = "{validation.required}")
        private String password;
    }

    @Getter @Builder
    public static class LoginResponse {
        // Token — có khi không cần MFA
        private String  accessToken;
        private String  refreshToken;
        private String  tokenType;
        private Instant accessTokenExpiresAt;
        private Instant refreshTokenExpiresAt;

        // MFA — có khi tài khoản bật MFA
        private boolean mfaRequired;
        private String  mfaType;
        private String  mfaPendingToken;

        // User info
        private UserInfo user;
    }

    @Getter @Builder
    public static class UserInfo {
        private UUID        userId;
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

    // =========================================================================
    // REFRESH TOKEN
    // =========================================================================

    @Getter @Setter
    public static class RefreshTokenRequest {

        @NotBlank(message = "{validation.required}")
        private String refreshToken;
    }

    @Getter @Builder
    public static class RefreshTokenResponse {
        private String  accessToken;
        private String  refreshToken;
        private String  tokenType;
        private Instant accessTokenExpiresAt;
        private Instant refreshTokenExpiresAt;
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    @Getter @Setter
    public static class LogoutRequest {

        @NotBlank(message = "{validation.required}")
        private String refreshToken;

        private boolean logoutAllDevices = false;
    }

    // =========================================================================
    // FORGOT / RESET PASSWORD
    // =========================================================================

    @Getter @Setter
    public static class ForgotPasswordRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        private String email;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {

        @NotBlank(message = "{validation.required}")
        private String token;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String newPassword;

        @NotBlank(message = "{validation.required}")
        private String confirmPassword;
    }

    // =========================================================================
    // CHANGE PASSWORD
    // =========================================================================

    @Getter @Setter
    public static class ChangePasswordRequest {

        @NotBlank(message = "{validation.required}")
        private String currentPassword;

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String newPassword;

        @NotBlank(message = "{validation.required}")
        private String confirmPassword;
    }

    // =========================================================================
    // EMAIL VERIFY
    // =========================================================================

    @Getter @Setter
    public static class VerifyEmailRequest {

        @NotBlank(message = "{validation.required}")
        private String token;
    }

    @Getter @Setter
    public static class ResendVerifyEmailRequest {

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.invalid.format}")
        private String email;
    }
}
