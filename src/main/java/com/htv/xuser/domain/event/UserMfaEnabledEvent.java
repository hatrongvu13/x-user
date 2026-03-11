package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.mfa-enabled
// TRIGGER: user bật MFA
// CONSUMERS:
//   - x-notification: gửi email xác nhận bật MFA
//   - x-audit: ghi log sự kiện bảo mật
// ─────────────────────────────────────────────────────────────────────────────
public class UserMfaEnabledEvent extends BaseUserEvent {
    public final String mfaType;   // TOTP | EMAIL_OTP

    public UserMfaEnabledEvent(String userId, String mfaType) {
        super("USER_MFA_ENABLED", userId);
        this.mfaType = mfaType;
    }
}
