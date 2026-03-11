package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.mfa-disabled
// TRIGGER: user tắt MFA
// CONSUMERS:
//   - x-notification: gửi email cảnh báo tắt MFA
//   - x-audit: ghi log sự kiện bảo mật
// ─────────────────────────────────────────────────────────────────────────────
public class UserMfaDisabledEvent extends BaseUserEvent {
    public final String disabledBy;

    UserMfaDisabledEvent(String userId, String disabledBy) {
        super("USER_MFA_DISABLED", userId);
        this.disabledBy = disabledBy;
    }
}
