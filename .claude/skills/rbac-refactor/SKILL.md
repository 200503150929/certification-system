---
name: rbac-refactor
description: 将工程教育专业认证系统的硬编码角色权限，改造为 RBAC 动态权限管理。涉及后端权限表设计 + 前端 Vue 3 对接。
---

# RBAC 权限重构技能

## 目标
将项目中所有 `user.role === 'xxx'` 的硬编码判断，替换为基于动态权限标识符（如 `user:delete`）的 RBAC 模型。

## 执行原则
- **先读后写**：每次动手前，先读取项目现有代码（Entity、Controller、Vue 组件），理解当前实现方式，再输出符合项目风格的代码。
- **分阶段确认**：每完成一个阶段，输出清单并等待用户确认，再进入下一阶段。

## 执行流程

### 阶段一：后端权限模型设计
1. 读取 `backend/src/main/java/com/certification/backend/entity/` 下的现有 Entity，理解项目的 JPA 编码风格（如是否用 Lombok、是否用 `@CreationTimestamp` 等）。
2. 参考项目现有 Entity 风格，设计三张表对应的 Entity：
   - `Permission`（权限标识符表）
   - `RolePermission`（角色-权限关联表）
   - `UserPermission`（用户-权限扩展表）
3. 设计对应的 Repository 接口（继承 `JpaRepository`）。
4. 设计 `PermissionDataInitializer`（实现 `ApplicationRunner`），在启动时预置权限数据。
5. 设计 `GET /api/permissions/current` 接口，返回当前用户的权限标识符列表。
6. 修改 JWT 生成逻辑，将权限列表写入 Token；修改 `@PreAuthorize` 注解，从 `hasRole` 改为 `hasAuthority`。
7. 输出新增/修改的文件清单，等待用户确认后进入阶段二。

### 阶段二：前端动态权限对接
1. 读取 `frontend/src` 下的现有代码，理解项目的状态管理方案（Pinia/ Vuex）、API 封装风格。
2. 按项目现有风格实现：
   - `permissionStore`（Pinia store，负责拉取和缓存权限列表）
   - `usePermission` 组合函数
   - `v-permission` 自定义指令
3. 修改登录流程，登录成功后拉取权限列表。
4. 修改路由守卫，从角色判断改为权限判断。
5. 全局搜索并替换所有 `user.role === 'xxx'` 的硬编码，改为权限判断。
6. 输出修改清单和测试验证点，等待用户确认。

## 质量约束
- 所有新增代码必须符合项目现有编码风格（缩进、命名、注解使用等）。
- 每阶段完成后必须输出完整文件清单，不得一次性修改全部代码。
- 替换硬编码时，先输出映射表（原硬编码 → 目标权限标识符），经用户确认后再执行替换。

## 提示
- 权限标识符统一使用 `<资源>:<操作>` 格式（如 `user:delete`）。
- `admin:*` 表示管理员拥有所有权限，仅限超级管理员。
- 教师默认权限：`course:*` + `grade:*` + `achievement:view`
- 学生默认权限：`course:list` + `grade:list`