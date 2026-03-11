package com.htv.xuser.domain.event;

import java.time.Instant;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.email-verified
// TRIGGER: user click link xác thực email thành công
// CONSUMERS:
//   - x-notification: gửi email chào mừng
//   - x-reward: tặng điểm cho user mới hoàn thành onboarding
// ─────────────────────────────────────────────────────────────────────────────
public class UserEmailVerifiedEvent extends BaseUserEvent {
    public final String email;
    public final Instant verifiedAt;

    public UserEmailVerifiedEvent(String userId, String email, Instant verifiedAt) {
        super("USER_EMAIL_VERIFIED", userId);
        this.email = email;
        this.verifiedAt = verifiedAt;
    }
}
