package com.htv.xuser.domain.event;

import java.time.Instant;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.password-changed
// TRIGGER: user đổi mật khẩu hoặc reset mật khẩu
// CONSUMERS:
//   - x-session: thu hồi tất cả sessions cũ (bảo mật)
//   - x-notification: gửi email cảnh báo đổi mật khẩu
// ─────────────────────────────────────────────────────────────────────────────
public class UserPasswordChangedEvent extends BaseUserEvent {
    public final String changedBy;     // "self" hoặc "admin" hoặc "reset-flow"
    public final Instant changedAt;

    public UserPasswordChangedEvent(String userId, String changedBy) {
        super("USER_PASSWORD_CHANGED", userId);
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }
}
