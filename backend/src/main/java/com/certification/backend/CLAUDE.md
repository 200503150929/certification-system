# EEAS 后端规则（certification-backend）

Spring Boot 3 + Spring Data JPA（Hibernate）+ MySQL 8。

# 包路径

根包统一为 `com.certification`，所有后端代码均位于此包下。无子模块拆分，所有能力（配置、控制器、业务、数据访问、实体、DTO、安全、工具、异常、枚举）均放在根包下的对应子包中。

启动类位于根包 `com.certification`；Repository 扫描由 JPA 自动完成（通过 `@EnableJpaRepositories` 或 Spring Boot 自动配置），无需额外指定。

# 分层规范
Controller → Service → Repository → 数据库


- **Controller**（`controller` 包）：参数接收、参数校验、调用 Service、返回统一响应。**禁止**编写业务逻辑。
- **Service / ServiceImpl**（`service` / `service.impl` 包）：核心业务逻辑层，事务边界在此层（`@Transactional`）。跨领域的组合逻辑也在此层编排。
- **Repository**（`repository` 包）：继承 `JpaRepository` 或 `JpaSpecificationExecutor`，仅做单表 CRUD 或简单查询。复杂查询使用 `@Query` 注解（JPQL 或原生 SQL），禁止字符串拼接 SQL。
- **Entity**（`entity` 包）：与表一一映射，字段名/类型与表保持一致。

# 实体类分工

- **Entity（PO）**：表映射对象，放 `entity` 包。时间字段使用统一的 JSON 格式化注解（`yyyy-MM-dd HH:mm:ss`，时区 GMT+8）。字段类型使用 `LocalDateTime`。
- **DTO（Request）**：接收前端入参，放 `dto/request` 包。需要校验时使用 Jakarta Validation 注解（`@NotNull`、`@Size` 等），支持分组校验。
- **VO（Response）**：返回前端的数据结构，放 `dto/response` 包。
- **Query**：分页查询参数，继承统一分页基类，可放 `dto/request` 或独立 `query` 包。模糊匹配字段以 `Fuzzy` 后缀约定（如 `nameFuzzy`）。

主键策略：业务实体多为 String UUID（培养方案、课程、课程目标、考核环节等），基础数据（如用户、角色）多为自增 Long。

# Controller 规范

- 类注解：`@RestController`、`@RequestMapping("/<模块路径>")`、`@Validated`。管理员接口必须加 `@PreAuthorize("hasRole('ADMIN')")`（需在 SecurityConfig 中开启 `@EnableMethodSecurity`）。
- 继承公共基础 Controller，使用其提供的成功/业务错误返回方法，**禁止**手动 new 响应对象。
- 全局路径上下文为 `/api`，由 `server.servlet.context-path` 统一配置，Controller 路径中**不再重复**写 `/api`。
- 入参：列表查询用 Query 对象（GET 请求 + `@ModelAttribute` 或 `@Valid`），写操作用 DTO 对象 + `@RequestBody`。
- 出参：始终使用统一的响应包装类型 `ResponseVO<T>`。

# 鉴权与登录态

- 统一使用 JWT Token，前端在请求头 `Authorization` 中携带（格式 `Bearer <token>`）。
- 登录校验由 `JwtAuthenticationFilter`（继承 `OncePerRequestFilter`）完成，解析 Token 并设置 `SecurityContextHolder`。
- 业务代码取当前登录用户**只能**通过 `SecurityContextHolder.getContext().getAuthentication()` 获取，禁止在 Controller / Service 中自行解析 Token。
- 权限拒绝由 Spring Security 自动处理（`AccessDeniedException`），由全局异常处理统一转响应。

# 统一返回与异常

- 所有 Controller 返回统一响应包装 `ResponseVO<T>`：包含 `status`（success/error）、`code`（状态码）、`info`（提示信息）、`data`（业务数据）。
- 业务错误码集中维护在 `ResultCodeEnum` 枚举中（如成功、系统错误、业务错误、未登录、无权限、参数错误等）。
- 业务异常一律抛自定义 `BusinessException`（可携带错误码），由 `GlobalExceptionHandler` 统一处理转响应。**禁止在 Controller / Service 中 catch 后吞掉错误**。

# 分页

- Query 对象继承统一分页基类，含页码、页大小、排序字段。
- 查询流程：使用 `PageRequest.of(pageNum - 1, pageSize, Sort.by(...))` 构建分页参数，调用 `JpaRepository.findAll(Pageable)` 或自定义分页查询，最后封装为统一分页结果 VO（含 `total`、`list`、`pageNum`、`pageSize`）。
- 默认页大小由枚举或常量提供（推荐 15 / 20 等档位）。

# 数据库与字段

- 库名：`certification_system`，字符集 `utf8mb4`。
- JPA 自动开启下划线到驼峰映射（默认行为，无需额外配置）。
- 公共字段：`created_at`、`updated_at` 使用 `@CreationTimestamp` / `@UpdateTimestamp` 自动填充；多数表有 `status` 状态字段（整数枚举：1 正常 / 0 停用）。**无统一的软删除字段**，按需在业务里判断。
- DDL 与基础数据 SQL 放在后端工程根目录（如 `sql/` 目录），作为版本化的初始化脚本。

# 工具类与公共组件

- Excel 工具（用于批量导入）：使用 EasyExcel 或 Apache POI，放 `util` 包。
- 日期格式化：通过全局 `Jackson2ObjectMapperBuilder` 配置统一格式。
- Redis：本项目暂不作强制依赖，如需缓存登录态或达成度计算结果，可后续引入 Redis 并封装独立组件。**禁止**在 Controller 直接操作 Redis。

# 开发规范

- Controller 只做"接收 → 调用 Service → 返回"；业务逻辑全部在 Service 层。
- 跨 Service 的组合也放 Service 层（无独立的 Biz 层）。
- Repository 复杂查询用 `@Query` 注解或方法命名约定，禁止字符串拼接 SQL。
- 公共能力（异常、枚举、常量、工具）放根包下的对应子包，无 common 模块概念。

# 禁止行为

- 禁止在 Controller 中编写业务逻辑或直接操作 Repository。
- 禁止将 Entity 直接返回给前端（必须封装为 ResponseVO 再返回）。
- 禁止在循环中调用 Repository（N+1 问题）。批量场景走批量方法（`saveAll`、`deleteAll`）或 IN 查询。
- 禁止吞异常或自定义响应结构绕过统一返回。
- 禁止管理端 Controller 不带 `@PreAuthorize` 权限注解。
- 禁止在 Controller / Service 中自行解析 Token，统一走 `SecurityContextHolder`。
- 禁止使用 MyBatis 或 MyBatis Plus 风格 API（本项目使用 Spring Data JPA）。
- 禁止不使用 `@Transactional` 或滥用事务（只读事务应标记 `readOnly = true`）。

# 子模块约束

本项目为单体后端，无子模块拆分。所有约束以本文件为准。

