package com.htv.xuser.domain.shared;

import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 5: UserRoleInfo
// Dùng bởi: x-audit, x-admin-dashboard
// Lấy từ: gRPC GetUserRoles
// ─────────────────────────────────────────────────────────────────────────────
public record UserRoleInfo(
        String userId,
        Set<RoleInfo> roles
) {
}

record RoleInfo(
        String roleId,
        String name,
        String description,
        Set<String> permissions
) {
}
