package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.activated
// TRIGGER: admin kích hoạt user hoặc user tự kích hoạt
// CONSUMERS:
//   - x-notification: gửi thông báo tài khoản đã kích hoạt
//   - x-session: cho phép tạo session
// ─────────────────────────────────────────────────────────────────────────────
public class UserActivatedEvent extends BaseUserEvent {
    public final String activatedBy;    // userId của admin, hoặc "self"

    public UserActivatedEvent(String userId, String activatedBy) {
        super("USER_ACTIVATED", userId);
        this.activatedBy = activatedBy;
    }
}
