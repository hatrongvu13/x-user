package com.htv.xuser.domain.shared;

import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 2: UserSummary
// Dùng bởi: x-order, x-notification, x-review...
// Lấy từ: gRPC GetUserById hoặc cache local
// ─────────────────────────────────────────────────────────────────────────────
public record UserSummary(
        UUID userId,
        String  email,
        String  username,
        String  firstName,
        String  lastName,
        String  fullName,       // firstName + " " + lastName
        String  avatarUrl,
        String  phoneNumber,
        String  status          // ACTIVE | INACTIVE | ...
) {}
