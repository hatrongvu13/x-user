package com.htv.xuser.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * UserEntity — bảng users
 * <p>
 * Tính năng:
 * - Đăng nhập bằng email + password
 * - MFA: TOTP (Google Authenticator) hoặc OTP qua email
 * - Xác thực email khi đăng ký
 * - Quản lý phiên: refresh token, thời gian login cuối
 * - Phân quyền: nhiều Role, mỗi Role nhiều Permission
 * - Soft delete + Optimistic locking (từ BaseEntity)
 */
@Entity
@Table(
        name = "users",
        schema = "public",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_username", columnList = "username", unique = true),
                @Index(name = "idx_users_status", columnList = "status"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {
    // ── Thông tin cơ bản ──────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // ── Trạng thái tài khoản ──────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    // ── Xác thực email ────────────────────────────────────────────────────────
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    // token gửi trong email xác thực (UUID, hết hạn sau X giờ)
    @Column(name = "email_verify_token", length = 100)
    private String emailVerifyToken;

    @Column(name = "email_verify_token_expires_at")
    private Instant emailVerifyTokenExpiresAt;

    // ── MFA ───────────────────────────────────────────────────────────────────
    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", length = 20)
    private MfaType mfaType; // TOTP hoặc EMAIL_OTP

    // secret key cho TOTP (Google Authenticator), encrypted at rest
    @Column(name = "mfa_totp_secret", length = 255)
    private String mfaTotpSecret;

    // backup codes dùng khi mất thiết bị TOTP (lưu dạng JSON array đã hash)
    @Column(name = "mfa_backup_codes", columnDefinition = "TEXT")
    private String mfaBackupCodes;

    // OTP gửi qua email (dùng khi mfaType = EMAIL_OTP)
    @Column(name = "mfa_otp_code", length = 10)
    private String mfaOtpCode;

    @Column(name = "mfa_otp_expires_at")
    private Instant mfaOtpExpiresAt;

    @Column(name = "mfa_otp_attempts", nullable = false)
    @Builder.Default
    private int mfaOtpAttempts = 0; // chống brute force

    // ── Quản lý mật khẩu ──────────────────────────────────────────────────────
    // token đặt lại mật khẩu (UUID, hết hạn sau 1 giờ)
    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private Instant passwordResetTokenExpiresAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    // ── Bảo mật đăng nhập ─────────────────────────────────────────────────────
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45) // IPv6 max 45 chars
    private String lastLoginIp;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil; // tạm khóa sau N lần sai password

    // ── Quan hệ: User ↔ Role (nhiều-nhiều) ───────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            indexes = {
                    @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
                    @Index(name = "idx_user_roles_role_id", columnList = "role_id"),
            }
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum UserStatus {
        PENDING,    // mới đăng ký, chưa xác thực email
        ACTIVE,     // hoạt động bình thường
        INACTIVE,   // tự tắt hoặc admin vô hiệu hóa
        SUSPENDED,  // bị khóa do vi phạm
        BANNED      // bị cấm vĩnh viễn
    }

    public enum MfaType {
        TOTP,       // Google Authenticator / Authy
        EMAIL_OTP   // OTP 6 số gửi qua email
    }

    // ── Helper methods ────────────────────────────────────────────────────────
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isDeleted() && !isLocked();
    }

    public void addRole(RoleEntity role) {
        this.roles.add(role);
    }

    public void removeRole(RoleEntity role) {
        this.roles.remove(role);
    }
}