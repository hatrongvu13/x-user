package com.htv.xuser.domain.event;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.profile-updated
// TRIGGER: user cập nhật thông tin cá nhân
// CONSUMERS:
//   - x-search: cập nhật index tìm kiếm
//   - x-order: cập nhật thông tin người đặt hàng
//   - x-notification: sync thông tin người nhận thông báo
// ─────────────────────────────────────────────────────────────────────────────
public class UserProfileUpdatedEvent extends BaseUserEvent {
    public final String firstName;
    public final String lastName;
    public final String phoneNumber;
    public final String avatarUrl;

    public UserProfileUpdatedEvent(String userId, String firstName, String lastName,
                            String phoneNumber, String avatarUrl) {
        super("USER_PROFILE_UPDATED", userId);
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }
}
