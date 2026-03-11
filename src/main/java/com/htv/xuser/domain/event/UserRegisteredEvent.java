package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.registered
// TRIGGER: user đăng ký tài khoản mới
// CONSUMERS:
//   - x-notification: gửi email xác thực
//   - x-analytics: theo dõi số lượng đăng ký
// ─────────────────────────────────────────────────────────────────────────────
public class UserRegisteredEvent extends BaseUserEvent {
    public final String email;
    public final String username;
    public final String firstName;
    public final String lastName;
    public final String emailVerifyToken;   // gửi kèm để notification service tạo link

    public UserRegisteredEvent(String userId, String email, String username,
                        String firstName, String lastName, String emailVerifyToken) {
        super("USER_REGISTERED", userId);
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailVerifyToken = emailVerifyToken;
    }
}
