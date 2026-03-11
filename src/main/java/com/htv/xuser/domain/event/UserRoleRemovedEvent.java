package com.htv.xuser.domain.event;

import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.role-removed
// TRIGGER: admin gỡ role khỏi user
// CONSUMERS:
//   - x-session: thu hồi token, buộc re-login để cập nhật permission
//   - x-audit: ghi log thay đổi phân quyền
// ─────────────────────────────────────────────────────────────────────────────
public class UserRoleRemovedEvent extends BaseUserEvent {
    public final String roleName;
    public final Set<String> currentRoles;
    public final Set<String> currentPermissions;
    public final String removedBy;

    public UserRoleRemovedEvent(String userId, String roleName,
                         Set<String> currentRoles, Set<String> currentPermissions,
                         String removedBy) {
        super("USER_ROLE_REMOVED", userId);
        this.roleName = roleName;
        this.currentRoles = currentRoles;
        this.currentPermissions = currentPermissions;
        this.removedBy = removedBy;
    }
}
