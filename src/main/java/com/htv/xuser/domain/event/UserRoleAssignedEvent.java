package com.htv.xuser.domain.event;

import java.util.Set;

// ─────────────────────────────────────────────────────────────────────────────
// TOPIC: xsystem.user.role-assigned
// TRIGGER: admin gán role mới cho user
// CONSUMERS:
//   - x-session: làm mới token để cập nhật permission mới
//   - x-audit: ghi log thay đổi phân quyền
// ─────────────────────────────────────────────────────────────────────────────
public class UserRoleAssignedEvent extends BaseUserEvent {
    public final String roleName;
    public final Set<String> currentRoles;   // snapshot toàn bộ roles sau khi gán
    public final Set<String> currentPermissions;
    public final String assignedBy;

    public UserRoleAssignedEvent(String userId, String roleName,
                          Set<String> currentRoles, Set<String> currentPermissions,
                          String assignedBy) {
        super("USER_ROLE_ASSIGNED", userId);
        this.roleName = roleName;
        this.currentRoles = currentRoles;
        this.currentPermissions = currentPermissions;
        this.assignedBy = assignedBy;
    }
}
