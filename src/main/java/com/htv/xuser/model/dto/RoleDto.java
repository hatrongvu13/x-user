package com.htv.xuser.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * RoleDto — Request/Response cho toàn bộ tính năng Role
 *
 * Tính năng:
 *  [1] Tạo role
 *  [2] Cập nhật role
 *  [3] Xem danh sách / chi tiết
 *  [4] Gán / gỡ permission vào role
 *  [5] Xóa role
 */
public final class RoleDto {

    private RoleDto() {}

    // =========================================================================
    // [1] TẠO ROLE
    // POST /api/v1/roles
    // =========================================================================

    @Getter
    @Setter
    public static class CreateRequest {

        @NotBlank(message = "{role.name.field}")
        @Size(min = 3, max = 50, message = "{validation.size.range}")
        @Pattern(
                regexp  = "^ROLE_[A-Z0-9_]+$",
                message = "{validation.invalid.format}"    // bắt buộc prefix ROLE_
        )
        private String name;

        @Size(max = 255)
        private String description;

        /** Gán permission ngay khi tạo — optional */
        private Set<UUID> permissionIds;
    }

    // =========================================================================
    // [2] CẬP NHẬT ROLE
    // PUT /api/v1/roles/{id}
    // =========================================================================

    @Getter
    @Setter
    public static class UpdateRequest {

        /**
         * Không cho đổi name của role (tránh ảnh hưởng đến permission check toàn hệ thống).
         * Chỉ cập nhật description và permissions.
         */
        @Size(max = 255)
        private String description;

        /**
         * Thay toàn bộ permissions của role.
         * null = giữ nguyên. Set rỗng = xóa hết permissions.
         */
        private Set<UUID> permissionIds;
    }

    // =========================================================================
    // [3] GÁN / GỠ PERMISSION VÀO ROLE
    // POST   /api/v1/roles/{id}/permissions
    // DELETE /api/v1/roles/{id}/permissions/{permissionId}
    // =========================================================================

    @Getter
    @Setter
    public static class AssignPermissionRequest {

        @NotNull(message = "{validation.required}")
        private UUID permissionId;
    }

    // =========================================================================
    // RESPONSE VARIANTS
    // =========================================================================

    // ─────────────────────────────────────────────────────────────────────────
    // Response — Chi tiết đầy đủ
    // Dùng: GET /api/v1/roles/{id}
    //       POST /api/v1/roles
    //       PUT  /api/v1/roles/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {
        private UUID        id;
        private String      name;
        private String      description;
        private boolean     system;         // true = không cho xóa/sửa name
        private int         userCount;      // số user đang có role này
        private Instant     createdAt;
        private Instant updatedAt;
        private Set<PermissionDto.Response> permissions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SummaryResponse — Danh sách / nhúng vào UserDto
    // Dùng: GET /api/v1/roles
    //       Nhúng trong UserDto.AdminResponse
    // ─────────────────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class SummaryResponse {
        private UUID        id;
        private String      name;
        private String      description;
        private boolean     system;
        private Set<String> permissions;   // chỉ tên: ["USER:READ", ...]
    }
}
