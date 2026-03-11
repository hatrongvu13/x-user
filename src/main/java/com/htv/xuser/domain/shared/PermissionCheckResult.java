package com.htv.xuser.domain.shared;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 4: PermissionCheckResult
// Dùng bởi: các service cần fine-grained authorization
// Lấy từ: gRPC CheckPermission
// ─────────────────────────────────────────────────────────────────────────────
public record PermissionCheckResult(
        boolean grant,
        String  reason  // mô tả tại sao granted/denied
) {
    public static PermissionCheckResult granted() {
        return new PermissionCheckResult(true, null);
    }

    public static PermissionCheckResult denied(String reason) {
        return new PermissionCheckResult(false, reason);
    }
}
