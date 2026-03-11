package com.htv.xuser.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * MfaDto — Request/Response cho nhóm tính năng MFA
 * <p>
 * Tính năng bao gồm:
 * [1] Xác minh MFA sau đăng nhập  (khi mfaRequired=true từ LoginResponse)
 * [2] Bật MFA — bước 1: khởi tạo (lấy QR hoặc gửi OTP test)
 * [3] Bật MFA — bước 2: xác nhận kích hoạt
 * [4] Tắt MFA
 * [5] Gửi lại OTP (cho EMAIL_OTP)
 * [6] Dùng backup code thay thế khi mất thiết bị
 * [7] Tái tạo backup codes
 */
public final class MfaDto {

    private MfaDto() {
    }

    // =========================================================================
    // [1] XÁC MINH MFA SAU ĐĂNG NHẬP
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/verify
     * <p>
     * Dùng sau khi login trả về mfaRequired=true.
     * mfaPendingToken lấy từ LoginResponse.mfaPendingToken.
     */
    @Getter
    @Setter
    public static class VerifyRequest {

        @NotBlank(message = "{validation.required}")
        private String mfaPendingToken;

        /**
         * Mã 6 số từ authenticator app (TOTP)
         * hoặc mã OTP nhận qua email (EMAIL_OTP).
         */
        @NotBlank(message = "{validation.required}")
        @Size(min = 6, max = 6, message = "{validation.size.range}")
        @Pattern(regexp = "^[0-9]{6}$", message = "{validation.invalid.format}")
        private String code;
    }

    @Getter
    @Builder
    public static class VerifyResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private AuthDto.UserInfo user;
    }

    // =========================================================================
    // [2] BẬT MFA — BƯỚC 1: KHỞI TẠO
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/enable
     * <p>
     * Yêu cầu: đã đăng nhập, chưa bật MFA.
     * Phải xác nhận mật khẩu hiện tại trước khi tiếp tục.
     */
    @Getter
    @Setter
    public static class EnableRequest {

        @NotBlank(message = "{validation.required}")
        private String password;   // xác nhận mật khẩu hiện tại

        @NotBlank(message = "{validation.required}")
        @Pattern(
                regexp = "^(TOTP|EMAIL_OTP)$",
                message = "{validation.invalid.format}"
        )
        private String mfaType;    // "TOTP" hoặc "EMAIL_OTP"
    }

    @Getter
    @Builder
    public static class EnableResponse {
        private String mfaType;    // "TOTP" | "EMAIL_OTP"

        // ── Chỉ có khi mfaType = TOTP ──
        /**
         * Base32 secret — hiển thị để user nhập thủ công vào app
         */
        private String totpSecret;
        /**
         * Data URI "data:image/png;base64,..." — hiển thị QR code để scan
         */
        private String totpQrCodeUri;
        /**
         * Tên hiển thị trong authenticator app: "x-user (user@email.com)"
         */
        private String totpIssuerLabel;

        // ── Chỉ có khi mfaType = EMAIL_OTP ──
        /**
         * Email nhận OTP test (che một phần: u***@gmail.com)
         */
        private String maskedEmail;

        private String message;    // "Nhập mã xác nhận để hoàn tất kích hoạt MFA"
    }

    // =========================================================================
    // [3] BẬT MFA — BƯỚC 2: XÁC NHẬN KÍCH HOẠT
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/enable/confirm
     * <p>
     * Xác nhận mã để hoàn tất bật MFA.
     */
    @Getter
    @Setter
    public static class ConfirmEnableRequest {

        @NotBlank(message = "{validation.required}")
        @Size(min = 6, max = 6)
        @Pattern(regexp = "^[0-9]{6}$", message = "{validation.invalid.format}")
        private String code;
    }

    @Getter
    @Builder
    public static class ConfirmEnableResponse {
        private boolean mfaEnabled;
        private String mfaType;

        /**
         * Backup codes — chỉ hiển thị đúng 1 lần tại đây.
         * Chỉ có khi mfaType = TOTP (không áp dụng cho EMAIL_OTP).
         * User PHẢI lưu lại, không thể xem lại sau này.
         */
        private List<String> backupCodes;

        private String message;
    }

    // =========================================================================
    // [4] TẮT MFA
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/disable
     */
    @Getter
    @Setter
    public static class DisableRequest {

        @NotBlank(message = "{validation.required}")
        private String password;  // xác nhận mật khẩu

        /**
         * Mã MFA hiện tại — bắt buộc để xác nhận chính chủ
         */
        @NotBlank(message = "{validation.required}")
        @Size(min = 6, max = 6)
        @Pattern(regexp = "^[0-9]{6}$", message = "{validation.invalid.format}")
        private String code;
    }

    @Getter
    @Builder
    public static class DisableResponse {
        private boolean mfaEnabled;  // luôn false
        private String message;
    }

    // =========================================================================
    // [5] GỬI LẠI OTP (EMAIL_OTP)
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/resend-otp
     * <p>
     * Dùng khi đang trong luồng đăng nhập (mfaPendingToken còn hạn)
     * hoặc khi đang trong luồng bật MFA (xác nhận kích hoạt).
     */
    @Getter
    @Setter
    public static class ResendOtpRequest {

        /**
         * mfaPendingToken từ LoginResponse — dùng trong luồng đăng nhập.
         * Nếu null, server hiểu là đang trong luồng bật MFA (user đã đăng nhập).
         */
        private String mfaPendingToken;
    }

    @Getter
    @Builder
    public static class ResendOtpResponse {
        private String maskedEmail;       // u***@gmail.com
        private String message;
        private int retryAfterSeconds; // thời gian chờ trước khi gửi lại
    }

    // =========================================================================
    // [6] DÙNG BACKUP CODE
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/backup-code
     * <p>
     * Dùng khi mất thiết bị TOTP.
     * Backup code chỉ dùng được 1 lần.
     */
    @Getter
    @Setter
    public static class BackupCodeRequest {

        @NotBlank(message = "{validation.required}")
        private String mfaPendingToken;

        @NotBlank(message = "{validation.required}")
        @Pattern(
                regexp = "^[A-Z0-9]{8}-[A-Z0-9]{4}$",
                message = "{validation.invalid.format}"
        )
        private String backupCode;         // format: XXXX XXXX (8+4 chars)
    }

    // Response: dùng lại MfaDto.VerifyResponse

    // =========================================================================
    // [7] TÁI TẠO BACKUP CODES (TOTP)
    // =========================================================================

    /**
     * POST /api/v1/auth/mfa/backup-codes/regenerate
     * <p>
     * Tái tạo toàn bộ backup codes mới.
     * Tất cả backup codes cũ bị vô hiệu ngay lập tức.
     */
    @Getter
    @Setter
    public static class RegenerateBackupCodesRequest {

        @NotBlank(message = "{validation.required}")
        private String password;

        @NotBlank(message = "{validation.required}")
        @Size(min = 6, max = 6)
        @Pattern(regexp = "^[0-9]{6}$", message = "{validation.invalid.format}")
        private String code; // mã TOTP hiện tại để xác nhận
    }

    @Getter
    @Builder
    public static class RegenerateBackupCodesResponse {
        private List<String> backupCodes;  // 8 codes mới
        private String message;
    }
}
