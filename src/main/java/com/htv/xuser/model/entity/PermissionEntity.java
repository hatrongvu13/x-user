package com.htv.xuser.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

/**
 * PermissionEntity — bảng permissions
 *
 * Dùng format: RESOURCE:ACTION
 * Ví dụ:
 *   USER:READ       — xem thông tin user
 *   USER:WRITE      — tạo/sửa user
 *   USER:DELETE     — xóa user
 *   ORDER:READ      — xem đơn hàng
 *   REPORT:EXPORT   — xuất báo cáo
 */
@Entity
@Table(
        name = "permissions",
        schema = "public",
        indexes = {
                @Index(name = "idx_permissions_name",     columnList = "name",     unique = true),
                @Index(name = "idx_permissions_resource",  columnList = "resource"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name; // VD: USER:READ

    @Column(nullable = false, length = 50)
    private String resource; // VD: USER, ORDER, REPORT

    @Column(nullable = false, length = 50)
    private String action; // VD: READ, WRITE, DELETE, EXPORT

    @Column(length = 255)
    private String description;
}

