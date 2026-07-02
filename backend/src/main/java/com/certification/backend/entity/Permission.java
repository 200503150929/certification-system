package com.certification.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限标识符实体
 *
 * 存储系统中所有可用的权限标识符（如 user:list、course:add）。
 * 权限标识符统一格式为 "<资源>:<操作>"，全部小写。
 */
@Entity
@Table(name = "permission")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 权限标识符，如 user:list、course:add */
    @Column(name = "permission_code", unique = true, nullable = false, length = 50)
    private String permissionCode;

    /** 权限名称，用于前端显示 */
    @Column(name = "permission_name", nullable = false, length = 50)
    private String permissionName;

    /** 所属模块 */
    @Column(length = 30)
    private String module;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
