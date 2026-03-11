package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.deleted
// TRIGGER: soft delete user
// CONSUMERS:
//   - x-session: thu hồi tất cả sessions
//   - x-order: đánh dấu các đơn hàng liên quan
//   - x-notification: hủy tất cả subscription
//   - x-search: xóa khỏi index
// ─────────────────────────────────────────────────────────────────────────────
public class UserDeletedEvent extends BaseUserEvent {
    public final String deletedBy;
    public final String email;      // lưu lại để cleanup các service khác

    public UserDeletedEvent(String userId, String email, String deletedBy) {
        super("USER_DELETED", userId);
        this.email = email;
        this.deletedBy = deletedBy;
    }
}
