package com.htv.xuser.model.dto;

import com.htv.xuser.model.entity.UserEntity.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * UserDto — Request/Response cho toàn bộ tính năng User
 * <p>
 * Tính năng:
 * [1] Tạo user (admin)
 * [2] Cập nhật user (admin)
 * [3] Xem / cập nhật profile bản thân
 * [4] Đổi trạng thái tài khoản
 * [5] Gán / gỡ role
 * [6] Tìm kiếm / lọc danh sách
 * [7] Xóa user
 * [8] Response variants — Profile, Summary, Detail, Admin
 */
public final class UserDto {

    private UserDto() {
    }

    // =========================================================================
    // [1] TẠO USER (ADMIN)
    // POST /api/v1/users
    // =========================================================================

    @Getter
    @Setter
    public static class CreateRequest {

        @NotBlank(message = "{user.email.field}")
        @Email(message = "{validation.invalid.format}")
        @Size(max = 255)
        private String email;

        @NotBlank(message = "{user.username.field}")
        @Size(min = 3, max = 50, message = "{validation.size.range}")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "{validation.invalid.format}"
        )
        private String username;

        @NotBlank(message = "{user.password.field}")
        @Size(min = 8, max = 100, message = "{validation.size.range}")
        private String password;

        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Pattern(
                regexp = "^\\+?[0-9]{9,15}$",
                message = "{validation.invalid.format}"
        )
        private String phoneNumber;

        @Size(max = 500)
        private String avatarUrl;

        /**
         * Gán role ngay khi tạo — optional. Nếu null → tự gán ROLE_USER mặc định
         */
        private Set<UUID> roleIds;
    }

    // =========================================================================
    // [2] CẬP NHẬT USER (ADMIN) + CẬP NHẬT PROFILE BẢN THÂN
    // PUT /api/v1/users/{id}
    // PUT /api/v1/users/me
    // =========================================================================

    /**
     * PATCH semantics — chỉ cập nhật field có giá trị (không null).
     */
    @Getter
    @Setter
    public static class UpdateRequest {

        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Pattern(
                regexp = "^\\+?[0-9]{9,15}$",
                message = "{validation.invalid.format}"
        )
        private String phoneNumber;

        @Size(max = 500)
        private String avatarUrl;
    }

    // =========================================================================
    // [3] ĐỔI TRẠNG THÁI
    // PATCH /api/v1/users/{id}/status
    // =========================================================================

    @Getter
    @Setter
    public static class UpdateStatusRequest {

        @NotNull(message = "{validation.required}")
        private UserStatus status;

        /**
         * Bắt buộc khi status = SUSPENDED hoặc BANNED
         */
        @Size(max = 500)
        private String reason;
    }

    // =========================================================================
    // [4] GÁN ROLE
    // POST /api/v1/users/{id}/roles
    // =========================================================================

    @Getter
    @Setter
    public static class AssignRoleRequest {

        @NotNull(message = "{validation.required}")
        private UUID roleId;
    }

    // =========================================================================
    // [5] TÌM KIẾM / LỌC DANH SÁCH
    // GET /api/v1/users?keyword=...&status=...&page=0&size=20
    // =========================================================================

    @Getter
    @Setter
    public static class SearchRequest {

        /**
         * Tìm trên email, username, firstName, lastName
         */
        private String keyword;

        private UserStatus status;

        private Boolean emailVerified;

        private Boolean mfaEnabled;

        /**
         * Lọc theo role cụ thể
         */
        private UUID roleId;

        private Instant createdFrom;

        private Instant createdTo;

        @Min(0)
        private int page = 0;

        @Min(1)
        @Max(100)
        private int size = 20;

        /**
         * Trường sắp xếp: createdAt | email | username | status
         */
        private String sortBy = "createdAt";

        /**
         * asc | desc
         */
        private String sortDir = "desc";
    }

    // Admin tạo user
    @Getter @Setter
    public static class AdminCreateRequest {
        @NotBlank @Email @Size(max = 255) private String email;
        @NotBlank @Size(min = 3, max = 50) private String username;
        @NotBlank @Size(min = 8, max = 100) private String password;
        @Size(max = 100) private String firstName;
        @Size(max = 100) private String lastName;
        private Set<UUID> roleIds;
    }

    // =========================================================================
    // RESPONSE VARIANTS
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // Response — Chi tiết đầy đủ
    // Dùng: GET /api/v1/users/{id}  (USER:READ)
    //       POST /api/v1/users      (USER:WRITE)
    //       PUT  /api/v1/users/{id} (USER:WRITE)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {
        private UUID id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;           // firstName + " " + lastName
        private String phoneNumber;
        private String avatarUrl;
        private UserStatus status;
        private boolean emailVerified;
        private Instant emailVerifiedAt;
        private boolean mfaEnabled;
        private String mfaType;            // "TOTP" | "EMAIL_OTP" | null
        private Instant lastLoginAt;
        private Instant passwordChangedAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Set<String> roles;              // ["ROLE_ADMIN", ...]
        private Set<String> permissions;        // ["USER:READ", ...]
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ProfileResponse — Profile bản thân
    // Dùng: GET /api/v1/users/me  (authenticated)
    //       PUT /api/v1/users/me  (authenticated)
    // Bỏ các field admin-only: lastLoginIp, failedLoginAttempts, lockedUntil
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class ProfileResponse {
        private UUID id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phoneNumber;
        private String avatarUrl;
        private UserStatus status;
        private boolean emailVerified;
        private boolean mfaEnabled;
        private String mfaType;
        private Instant lastLoginAt;
        private Set<String> roles;
        private Set<String> permissions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SummaryResponse — Danh sách, payload nhẹ
    // Dùng: GET /api/v1/users  (USER:READ)
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class SummaryResponse {
        private UUID id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String avatarUrl;
        private UserStatus status;
        private boolean emailVerified;
        private boolean mfaEnabled;
        private Instant createdAt;
        private Set<String> roles;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AdminResponse — Dành cho admin dashboard
    // Bổ sung: lastLoginIp, failedLoginAttempts, lockedUntil, createdBy
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class AdminResponse {
        private UUID id;
        private String email;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phoneNumber;
        private String avatarUrl;
        private UserStatus status;
        private boolean emailVerified;
        private Instant emailVerifiedAt;
        private boolean mfaEnabled;
        private String mfaType;
        private Instant lastLoginAt;
        private String lastLoginIp;        // admin-only
        private int failedLoginAttempts; // admin-only
        private Instant lockedUntil;         // admin-only
        private Instant passwordChangedAt;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
        private Set<RoleDto.SummaryResponse> roles; // kèm permissions
    }

    /** Chi tiết đầy đủ — admin */
    @Getter @Builder
    public static class DetailResponse {
        private UUID        id;
        private String      email;
        private String      username;
        private String      firstName;
        private String      lastName;
        private String      fullName;
        private String      phoneNumber;
        private String      avatarUrl;
        private UserStatus  status;
        private boolean     emailVerified;
        private Instant     emailVerifiedAt;
        private boolean     mfaEnabled;
        private String      mfaType;
        private int         failedLoginAttempts;
        private Instant     lockedUntil;
        private Instant     lastLoginAt;
        private String      lastLoginIp;
        private Instant     passwordChangedAt;
        private Instant     createdAt;
        private Instant     updatedAt;
        private String      createdBy;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
