package com.certification.backend.config;

import com.certification.backend.entity.Permission;
import com.certification.backend.entity.Role;
import com.certification.backend.entity.RolePermission;
import com.certification.backend.repository.PermissionRepository;
import com.certification.backend.repository.RolePermissionRepository;
import com.certification.backend.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 权限数据初始化器
 *
 * 系统启动时自动检查并初始化 permission / role_permission 表的基础数据。
 * 仅在数据为空时写入，不会覆盖已有数据。
 */
@Component
public class PermissionDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionDataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionDataInitializer(PermissionRepository permissionRepository,
                                     RoleRepository roleRepository,
                                     RolePermissionRepository rolePermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (permissionRepository.count() > 0) {
            log.info("权限数据已存在，跳过初始化");
            return;
        }
        log.info("初始化权限数据...");

        // ============ 1. 定义所有权限 ============
        List<Permission> allPermissions = Arrays.asList(
                // ---- 仪表盘 ----
                createPermission("dashboard:view", "查看仪表盘", "dashboard", 1),

                // ---- 用户管理 ----
                createPermission("user:list", "用户列表", "user", 10),
                createPermission("user:add", "添加用户", "user", 11),
                createPermission("user:update", "编辑用户", "user", 12),
                createPermission("user:delete", "删除用户", "user", 13),
                createPermission("user:reset-password", "重置密码", "user", 14),
                createPermission("user:import", "批量导入用户", "user", 15),

                // ---- 角色权限管理 ----
                createPermission("role:list", "角色列表", "role", 20),
                createPermission("role:update", "编辑角色权限", "role", 21),

                // ---- 培养方案管理 ----
                createPermission("program:manage", "管理培养方案", "program", 30),
                createPermission("program:detail", "查看专业详情", "program", 31),
                createPermission("program:view", "查看培养方案(只读)", "program", 32),
                createPermission("program:add", "添加专业", "program", 33),
                createPermission("program:update", "编辑专业", "program", 34),
                createPermission("program:delete", "删除专业", "program", 35),
                createPermission("program:publish", "发布培养方案", "program", 36),

                // ---- 课程体系管理 ----
                createPermission("course:manage", "管理课程体系", "course", 40),
                createPermission("course:list", "查看课程列表", "course", 41),
                createPermission("course:detail", "查看课程详情", "course", 42),
                createPermission("course:add", "添加课程", "course", 43),
                createPermission("course:update", "编辑课程", "course", 44),
                createPermission("course:delete", "删除课程", "course", 45),
                createPermission("course:support-matrix", "管理支撑矩阵", "course", 46),

                // ---- 课程教学管理（教师） ----
                createPermission("course:teach", "管理授课课程", "teaching", 50),
                createPermission("course:student-list", "查看学生名单", "teaching", 51),
                createPermission("course:set-goals", "设置课程目标", "teaching", 52),
                createPermission("course:set-assessment", "设置考核方式", "teaching", 53),
                createPermission("course:enter-grade", "录入成绩", "teaching", 54),
                createPermission("course:upload-resource", "上传课程资源", "teaching", 55),

                // ---- 个人信息 ----
                createPermission("profile:view", "查看个人信息", "profile", 60),
                createPermission("profile:edit", "编辑个人信息", "profile", 61),
                createPermission("profile:change-password", "修改密码", "profile", 62),

                // ---- 达成度分析 ----
                createPermission("achievement:view", "查看达成度数据", "achievement", 70),
                createPermission("achievement:export", "导出报表", "achievement", 71)
        );

        permissionRepository.saveAll(allPermissions);
        log.info("已初始化 {} 条权限记录", allPermissions.size());

        // ============ 2. 角色-权限映射 ============
        Map<String, List<String>> rolePermissionMap = new HashMap<>();

        // 超级管理员：所有权限
        rolePermissionMap.put("admin", Arrays.asList(
                "dashboard:view",
                "user:list", "user:add", "user:update", "user:delete", "user:reset-password", "user:import",
                "role:list", "role:update",
                "program:manage", "program:detail", "program:view", "program:add", "program:update", "program:delete", "program:publish",
                "course:manage", "course:list", "course:detail", "course:add", "course:update", "course:delete", "course:support-matrix",
                "course:teach", "course:student-list", "course:set-goals", "course:set-assessment", "course:enter-grade", "course:upload-resource",
                "profile:view", "profile:edit", "profile:change-password",
                "achievement:view", "achievement:export"
        ));

        // 教师
        rolePermissionMap.put("teacher", Arrays.asList(
                "dashboard:view",
                "program:view",
                "course:list", "course:detail",
                "course:teach", "course:student-list", "course:set-goals", "course:set-assessment", "course:enter-grade", "course:upload-resource",
                "profile:view", "profile:edit", "profile:change-password",
                "achievement:view", "achievement:export"
        ));

        // 学生
        rolePermissionMap.put("student", Arrays.asList(
                "program:view",
                "course:detail", "course:list",
                "profile:view", "profile:edit", "profile:change-password"
        ));

        // 写入角色-权限关联
        for (Map.Entry<String, List<String>> entry : rolePermissionMap.entrySet()) {
            String roleName = entry.getKey();
            Role role = roleRepository.findByRoleName(roleName).orElse(null);
            if (role == null) {
                log.warn("角色 {} 不存在，跳过权限分配", roleName);
                continue;
            }

            List<String> permissionCodes = entry.getValue();
            for (String code : permissionCodes) {
                Permission perm = permissionRepository.findByPermissionCode(code).orElse(null);
                if (perm == null) {
                    log.warn("权限 {} 不存在，跳过", code);
                    continue;
                }

                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(perm.getId());
                rolePermissionRepository.save(rp);
            }
            log.info("角色 {} 已分配 {} 个权限", roleName, permissionCodes.size());
        }

        log.info("权限数据初始化完成");
    }

    private Permission createPermission(String code, String name, String module, Integer sortOrder) {
        Permission p = new Permission();
        p.setPermissionCode(code);
        p.setPermissionName(name);
        p.setModule(module);
        p.setSortOrder(sortOrder);
        return p;
    }
}
