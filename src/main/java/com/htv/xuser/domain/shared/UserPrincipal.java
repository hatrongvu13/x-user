package com.htv.xuser.domain.shared;

import java.util.Set;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 1: UserPrincipal
// Dùng bởi: TẤT CẢ services sau khi validate JWT
// Lấy từ: JWT claims hoặc gRPC ValidateTokenResponse
// ─────────────────────────────────────────────────────────────────────────────
public record UserPrincipal(
        UUID userId,
        String email,
        String username,
        Set<String> roles,          // ["ROLE_ADMIN", "ROLE_USER"]
        Set<String> permissions     // ["USER:READ", "ORDER:WRITE"]
) {
    // Helper — không cần gọi gRPC thêm
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }
}
