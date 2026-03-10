package com.htv.xuser.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * RoleEntity — bảng roles
 *
 * Ví dụ: ROLE_ADMIN, ROLE_USER, ROLE_MODERATOR
 * Một Role có nhiều Permission.
 * Một User có nhiều Role.
 */
@Entity
@Table(
        name = "roles",
        schema = "public",
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name; // VD: ROLE_ADMIN

    @Column(length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean system = false; // true = role mặc định, không cho xóa

    // ── Quan hệ: Role ↔ Permission (nhiều-nhiều) ──────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns        = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"),
            indexes = {
                    @Index(name = "idx_role_permissions_role_id",       columnList = "role_id"),
                    @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id"),
            }
    )
    @Builder.Default
    private Set<PermissionEntity> permissions = new HashSet<>();

    // ── Quan hệ ngược: Role ↔ User ────────────────────────────────────────────
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserEntity> users = new HashSet<>();

    // ── Helpers ───────────────────────────────────────────────────────────────
    public void addPermission(PermissionEntity permission) {
        this.permissions.add(permission);
    }

    public void removePermission(PermissionEntity permission) {
        this.permissions.remove(permission);
    }
}