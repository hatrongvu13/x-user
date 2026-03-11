package com.htv.xuser.domain.event;

import java.time.Instant;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.logged-in
// TRIGGER: đăng nhập thành công
// CONSUMERS:
//   - x-analytics: tracking login activity
//   - x-security: cập nhật last known IP, phát hiện đăng nhập bất thường
//   - x-notification: cảnh báo nếu đăng nhập từ thiết bị / địa điểm mới
// ─────────────────────────────────────────────────────────────────────────────
public class UserLoggedInEvent extends BaseUserEvent {
    public final String ip;
    public final String userAgent;
    public final Instant loginAt;

    public UserLoggedInEvent(String userId, String ip, String userAgent) {
        super("USER_LOGGED_IN", userId);
        this.ip = ip;
        this.userAgent = userAgent;
        this.loginAt = Instant.now();
    }
}
