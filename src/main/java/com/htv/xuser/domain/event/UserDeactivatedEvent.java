package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.deactivated
// TRIGGER: admin vô hiệu hóa user
// CONSUMERS:
//   - x-session: thu hồi tất cả active sessions
//   - x-notification: gửi thông báo tài khoản bị vô hiệu hóa
//   - x-order: dừng xử lý đơn hàng pending của user
// ─────────────────────────────────────────────────────────────────────────────
public class UserDeactivatedEvent extends BaseUserEvent {
    public final String reason;
    public final String deactivatedBy;
    public final String newStatus;     // INACTIVE | SUSPENDED | BANNED

    public UserDeactivatedEvent(String userId, String reason, String deactivatedBy, String newStatus) {
        super("USER_DEACTIVATED", userId);
        this.reason = reason;
        this.deactivatedBy = deactivatedBy;
        this.newStatus = newStatus;
    }
}
