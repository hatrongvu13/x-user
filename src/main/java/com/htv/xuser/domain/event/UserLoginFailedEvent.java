package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.login-failed
// TRIGGER: đăng nhập sai mật khẩu
// CONSUMERS:
//   - x-security: phát hiện brute-force, trigger CAPTCHA hoặc block IP
//   - x-notification: cảnh báo khi N lần sai liên tiếp
//   - x-audit: ghi log bảo mật
// ─────────────────────────────────────────────────────────────────────────────
public class UserLoginFailedEvent extends BaseUserEvent {
    public final String ip;
    public final int attemptCount;
    public final String reason;    // WRONG_PASSWORD | ACCOUNT_LOCKED | MFA_FAILED

    UserLoginFailedEvent(String userId, String ip, int attemptCount, String reason) {
        super("USER_LOGIN_FAILED", userId);
        this.ip = ip;
        this.attemptCount = attemptCount;
        this.reason = reason;
    }
}
