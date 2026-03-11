package com.htv.xuser.domain.shared;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED ENUM: UserStatus (dùng trong các service cần biết trạng thái user)
// ─────────────────────────────────────────────────────────────────────────────
public enum SharedUserStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    BANNED;

    public boolean isOperational() {
        return this == ACTIVE;
    }
}
