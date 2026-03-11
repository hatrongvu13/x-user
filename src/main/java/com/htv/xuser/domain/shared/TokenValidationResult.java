package com.htv.xuser.domain.shared;

import java.time.Instant;
import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 3: TokenValidationResult
// Dùng bởi: API Gateway, x-resource-server
// Lấy từ: gRPC ValidateToken
// ─────────────────────────────────────────────────────────────────────────────
public record TokenValidationResult(
        boolean      valid,
        String       userId,
        String       email,
        Set<String> roles,
        Set<String>  permissions,
        Instant expiresAt,
        String       failureReason   // null nếu valid = true
) {
    public static TokenValidationResult invalid(String reason) {
        return new TokenValidationResult(false, null, null,
                Set.of(), Set.of(), null, reason);
    }
}
