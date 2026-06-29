package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色-权限关联实体
 *
 * 建立角色与权限标识符之间的多对多关系。
 * 一个角色可以拥有多个权限，一个权限可以属于多个角色。
 */
@Entity
@Table(name = "role_permission")
@Data
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色ID，关联 role 表 */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** 权限ID，关联 permission 表 */
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
