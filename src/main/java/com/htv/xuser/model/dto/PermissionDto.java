package com.htv.xuser.model.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * PermissionDto — Request/Response cho toàn bộ tính năng Permission
 *
 * Tính năng:
 *  [1] Tạo permission
 *  [2] Cập nhật permission
 *  [3] Xem danh sách / chi tiết / lọc theo resource
 *  [4] Xóa permission
 */
public final class PermissionDto {

    private PermissionDto() {}

    // =========================================================================
    // [1] TẠO PERMISSION
    // POST /api/v1/permissions
    // =========================================================================

    @Getter
    @Setter
    public static class CreateRequest {

        /**
         * Phần resource của permission.
         * Ví dụ: USER, ORDER, REPORT, PAYMENT
         */
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 50, message = "{validation.size.range}")
        @Pattern(
                regexp  = "^[A-Z][A-Z0-9_]*$",
                message = "{validation.invalid.format}"
        )
        private String resource;

        /**
         * Phần action của permission.
         * Ví dụ: READ, WRITE, DELETE, EXPORT, APPROVE
         */
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 50, message = "{validation.size.range}")
        @Pattern(
                regexp  = "^[A-Z][A-Z0-9_]*$",
                message = "{validation.invalid.format}"
        )
        private String action;

        @Size(max = 255)
        private String description;

        // name tự sinh = resource + ":" + action  (VD: USER:READ)
    }

    // =========================================================================
    // [2] CẬP NHẬT PERMISSION
    // PUT /api/v1/permissions/{id}
    // =========================================================================

    @Getter
    @Setter
    public static class UpdateRequest {

        /**
         * Chỉ cho cập nhật description.
         * Không cho sửa resource/action vì name là contract đã được dùng trong code.
         */
        @Size(max = 255)
        private String description;
    }

    // =========================================================================
    // [3] TÌM KIẾM
    // GET /api/v1/permissions?resource=USER&action=READ
    // =========================================================================

    @Getter
    @Setter
    public static class SearchRequest {

        /** Lọc theo resource: USER, ORDER... */
        private String resource;

        /** Lọc theo action: READ, WRITE... */
        private String action;

        @Min(0)
        private int    page = 0;

        @Min(1) @Max(100)
        private int    size = 50;
    }

    // =========================================================================
    // RESPONSE
    // =========================================================================

    @Getter
    @Builder
    public static class Response {
        private UUID id;
        private String  name;           // RESOURCE:ACTION
        private String  resource;
        private String  action;
        private String  description;
        private int     roleCount;      // số role đang gán permission này
        private Instant createdAt;
        private Instant updatedAt;
    }
}

