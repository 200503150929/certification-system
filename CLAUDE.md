# 工程教育专业认证系统项目规则（certification-system）

工程教育专业认证智能服务系统（Engineering Education Accreditation System），对标《工程教育认证标准》，覆盖培养方案制定 → 课程体系建设 → 教学运行实施 → 考核评价 → 达成度核算全链路。

前后端分离，后端为单体 Spring Boot 应用（模块统一在一个工程中），前端为 Vue 3 单页应用（一个前端工程，通过路由区分角色页面）。

# 项目结构

后端 `backend/`（Spring Boot 3 + Maven 单体）：
- 根包 `com.certification`，所有 Java 代码均位于此包下。
- 分层结构（详见下文“分层与包结构”），按业务模块（模块一～模块七）组织 Controller / Service / Repository。
- 无子模块拆分，所有公共能力（实体、DTO、工具、异常、安全）均放在根包下的对应子包中。

前端 `frontend/`（Vue 3 + Vite）：
- 单页应用，通过路由和角色权限控制不同用户（管理员、教师、学生）的页面访问。
- 页面目录按角色划分：`src/views/admin/`、`src/views/teacher/`、`src/views/student/`。

> 包名统一为 `com.certification`，新增类必须落到正确的子包（如 `controller`、`service`、`repository` 等），不要混放。

# 系统角色

- 系统管理员：系统基础配置、角色权限、培养方案管理、课程体系管理、支撑矩阵配置、师生账号管理（批量导入）。
- 教师：管理所授课程的基本信息、课程目标设计、目标-指标点映射、课程资源、考核方式与成绩登记，查看达成度数据。
- 学生：查看个人所选课程列表及课程详情，查看个人信息。

角色字段约定在 `User.role`（字符串：`admin` / `teacher` / `student`）。

# 接口与路径

- 全局上下文：`/api`，由 `server.servlet.context-path` 提供，路径中**不再重复**写 `/api`。
- 路径形式：`/api/<模块路径>/<动作>`，模块路径示例：
  - 认证接口：`/auth/login`（无模块前缀）
  - 管理员接口：`/admin/users`、`/admin/programs`、`/admin/courses` 等
  - 教师接口：`/teacher/courses`、`/teacher/grades` 等
  - 学生接口：`/student/courses`、`/student/profile` 等
- 常用动作：`list`、`add`、`update`、`delete`、`detail`、`options`（下拉用）。

# 鉴权与权限

- 登录认证使用 JWT Token，前端在 `Authorization` 头中携带（格式 `Bearer <token>`）。
- 自定义 `JwtAuthenticationFilter` 拦截所有请求，校验 Token 并设置 `SecurityContextHolder`。
- 使用 Spring Security 的 `@PreAuthorize` 注解进行方法级权限控制（如 `@PreAuthorize("hasRole('ADMIN')")`）。
- 当前登录用户通过 `SecurityContextHolder.getContext().getAuthentication()` 获取，**禁止**在 Controller / Service 中手动解析 Token。

# 权限模型与编码约定

## 当前权限实现方式（重构前）
- 前端权限判断：通过 `User.role` 字段进行硬编码条件渲染（如 `v-if="user.role === 'admin'"`）。
- 后端权限控制：使用 `@PreAuthorize("hasRole('ADMIN')")` 注解，角色与权限点强绑定。
- 权限数据来源：仅依赖 JWT Token 中的角色信息，无独立权限表或动态权限列表。

## 目标权限模型（重构后-RBAC）
- 权限数据来源：后端提供独立权限接口（如 `/api/permissions`），返回当前用户的**权限标识符列表**（如 `["user:delete", "course:add"]`）。
- 前端权限控制：
  - 按钮级：使用自定义指令 `v-permission="'user:delete'"` 控制 DOM 显隐。
  - 路由级：使用路由守卫 + 权限列表动态过滤菜单和可访问页面。
- 后端权限控制：仍使用 `@PreAuthorize`，但权限表达式从 `hasRole('ADMIN')` 改为 `hasAuthority('user:delete')`，权限数据从数据库动态加载。
- 角色仅作为权限集合的载体，不再在代码中硬编码判断 `role === 'admin'`。

## 权限标识符命名规范
- 格式：`<资源>:<操作>`，全部小写。
- 示例：
  - 用户管理：`user:add`、`user:delete`、`user:update`、`user:list`
  - 课程管理：`course:add`、`course:delete`、`course:update`、`course:list`
  - 培养方案：`program:add`、`program:delete`、`program:update`、`program:list`
- 特殊权限：`*:*` 表示超级管理员（仅限系统管理员角色拥有）。

# 核心业务域（对应七大模块）

- **模块一（系统基础与权限管理）**：登录、用户管理（增删改查、批量导入、密码重置）、角色权限。
- **模块二（人才培养方案管理）**：专业管理、培养目标、毕业要求、指标点、培养目标-毕业要求支撑矩阵。
- **模块三（课程体系管理）**：课程基础信息（模块、代码、学分、学时等）、课程-毕业要求支撑矩阵。
- **模块四（课程教学管理）**：课程基本信息设置、课程目标设计、课程目标-指标点映射、课程资源、考核管理（考核方式、评分细则）、成绩登记。
- **模块五（达成度计算与分析）**：课程目标达成度、毕业要求指标点达成度、毕业要求达成度、达标判定、报表导出。
- **模块六（数据可视化看板）**：核心 KPI、达成度可视化图表、成绩分布、预警提醒。
- **模块七（个人信息与课程查看）**：教师/学生个人信息维护，学生查看选课列表及课程详情。

# 业务规则

- **培养方案**：一个专业对应一套培养方案，培养目标、毕业要求、指标点均属于该专业（通过 `program_id` 关联）。
- **支撑矩阵**：
  - 培养目标与毕业要求之间为“强/弱支撑”（`objective_requirement_matrix` 表）。
  - 课程与毕业要求指标点之间为 H/M/L 支撑（`course_requirement_matrix` 表）。
  - 课程目标与指标点之间为支撑强度（`objective_indicator_matrix` 表），用于达成度计算。
- **课程归属**：每门课程归属一名教师（`teacher_id`）；教师只能管理自己名下的课程。
- **考核与成绩**：每个课程可配置多个考核环节（作业、考试、实验等），每个考核环节关联一个或多个课程目标，并设置评分细则。成绩录入后自动进入达成度计算池。
- **达成度计算**：基于考核成绩、环节-课程目标映射、课程目标权重，计算每位学生的课程目标达成值，再汇总到毕业要求指标点达成度。


# 统一响应与异常

- 后端统一返回 `R<T>`（含 `code`、`msg`、`data`）。提供 `R.ok(data)` 和 `R.error(code, msg)`。
- 业务异常使用 `BusinessException`（可携带错误码），在 Service 层抛出。
- 全局异常处理捕获所有异常，返回统一格式 `R`。错误码统一维护在 `ResultCodeEnum`。

# 字段与返回约定

- 数据库字段下划线命名，Java 字段驼峰，由 JPA 自动映射。
- 前端字段保持与后端 VO 字段完全一致（驼峰），禁止前端重命名。
- 时间字段统一格式化为 `yyyy-MM-dd HH:mm:ss`（GMT+8），使用 `@JsonFormat` 或全局配置。
- 分页参数使用统一 `PageQuery`（含 `pageNum`、`pageSize`、排序字段），分页结果使用 `PageResult<T>`。

# 禁止行为

- 禁止 Controller 中编写业务逻辑或直接操作 Repository。
- 禁止返回 Entity 给前端（必须封装为 VO）。
- 禁止前后端字段不一致或私自重命名。
- 禁止生成 mock 数据，联调以真实接口为准。
- 禁止管理端接口不带权限注解（`@PreAuthorize`）。
- 禁止学生端接口暴露管理端字段（如创建人、审核状态等）。
- 禁止在循环中调用 Repository（N+1 问题），应使用联查或批量方法。
- 禁止吞异常（必须抛出或记录日志）。
- 禁止在 Service 中手动解析 Token（使用 `SecurityContextHolder`）。
- 禁止使用 `@Autowired` 字段注入（推荐构造器注入）。
- 禁止不写 `@Transactional`（Service 层实现类必须声明）。

# 子规则索引

- 后端详细规范：`backend/CLAUDE.md`
- 前端开发规范：`frontend/CLAUDE.md`（如有，需另行维护）