# Admin 端 · 架构说明

> 用途：本文件是本端实现的**技术契约**，用于约束代码生成与后续维护，不作为团队交付物对外。
> 范围：仅覆盖管理员端（Epic AD）。业务端（PM / Member）结构由对应负责人补齐。

---

## 1. 技术选型

| 层 | 选型 | 版本 | 理由 |
|---|---|---|---|
| 运行时 | Java | 21 (LTS) | 本机已装 Zulu 21 |
| 构建 | Maven | 3.9 | 本机已装 |
| 框架 | Spring Boot | 3.2.x | 方案 A1.2 / A2.1 明确要求 **AOP 切面**，Spring 原生支持 |
| ORM | MyBatis-Plus | 3.5.5 | 管理端 CRUD 密集，省掉大量模板代码 |
| 数据库 | MySQL | 8.0 | 本机已装 8.0.44 |
| 认证 | JWT (jjwt 0.12.x) | — | 无状态，三端共用同一套 |
| 前端 | Vue 3 + Vite | 3.4 / 5.x | — |
| UI | Element Plus | 2.x | 管理端以表格/表单/树为主，组件现成 |
| 状态 | Pinia | 2.x | 只存 token + 当前用户 |
| 请求 | Axios | 1.x | 统一封装拦截器 |

### 1.1 有意不选的东西

| 不用 | 原因 |
|---|---|
| **Spring Security** | 课设用它过重；方案要的是"注解 + AOP 拦截"，自建 20 行拦截器即可，且更直观可控 |
| **多租户框架** | 单企业形态（见 PROJECT_BRIEF），无 `tenant_id` |
| **Redis** | 本期无缓存/分布式会话需求，JWT 无状态已够 |
| **动态权限 / 权限配置表** | 方案 Table 5 明确列为 Won't |
| **国密算法** | 本期延期 |

---

## 2. 模块划分

```
com.aimanage
├── AiManageApplication.java      启动类
├── common/                       横切基础设施
│   ├── R.java                    统一响应体 {code, msg, data}
│   ├── BizException.java         业务异常
│   └── GlobalExceptionHandler.java
├── security/                     ★ 鉴权（对应方案 A1.2）
│   ├── RoleEnum.java             ADMIN / PM / MEMBER 三值枚举
│   ├── RequireRole.java          自定义注解，标注在 Controller 类或方法上
│   ├── LoginUser.java            当前登录用户快照
│   ├── UserContext.java          ThreadLocal 持有 LoginUser
│   ├── JwtUtil.java              签发 / 解析
│   └── AuthInterceptor.java      ★ 拦截器：校验 JWT + 校验角色
├── config/
│   └── WebConfig.java            注册拦截器，声明放行路径
├── auth/                         认证（三端共用）
│   ├── AuthController.java       POST /api/auth/login | logout | GET /me
│   └── dto/LoginRequest.java
├── entity/                       表实体
│   └── User.java
├── mapper/
│   └── UserMapper.java
└── admin/                        ★ Admin 端专属接口
    ├── AdminUserController.java  /api/admin/users/**
    └── AdminUserService.java
```

**分层原则**：`admin/` 下所有 Controller 的路径必须以 `/api/admin/` 开头——这是拦截器判定"管理端接口"的唯一依据。

---

## 3. 鉴权数据流（★ 本端安全的核心）

```
[浏览器]  POST /api/auth/login  {username, password}
             │
             ▼
[AuthController] 查 user 表 → 校验 BCrypt 密码 → 校验 status=1
             │
             ▼
[JwtUtil] 签发 token，payload 含 {userId, username, role}
             │
             ▼  返回 {token, user:{id,name,role}}
       前端存 localStorage
             │
             ▼  后续请求头 Authorization: Bearer <token>
             │
             ▼
[AuthInterceptor]  ← 注册在 /api/** 上
   1. 取 header，无 token → 401
   2. 解析签名 / 过期 → 401
   3. 写入 UserContext (ThreadLocal)
   4. 若路径以 /api/admin/ 开头：
        role != ADMIN → 403   ← ★ 安全边界就在这一行
   5. 放行，afterCompletion 清理 ThreadLocal
```

### 3.1 三条铁律

1. **前端路由守卫只是体验层**，安全边界 100% 在后端拦截器。方案 DoD 第 2 条要求"Member 越权访问被 403 拒绝"测的就是这条。
2. **Admin 不得写业务数据**。`/api/admin/**` 下不提供任何业务写接口（任务、需求、看板、甘特）。业务端的 GET 接口对 ADMIN 放行读、拒绝写。
3. **拦截器优先于 Controller**。任何新增的 `/api/admin/**` 接口自动获得角色保护，无需逐个加注解。

---

## 4. 目录结构

```
软件项目管理/
├── README.md
├── .gitignore
├── admin端_故事地图与任务分解.md      ← 需求侧（已完成）
├── docs/admin/
│   ├── ARCHITECTURE.md               ← 本文件
│   └── API_SPEC.md                   ← 接口契约
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/aimanage/...
│       └── resources/
│           ├── application.yml
│           └── db/
│               ├── schema.sql        ← 表结构唯一真源
│               └── data.sql          ← 种子数据（含首个 ADMIN）
└── frontend-admin/                   ← 管理端独立前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js           ← 路由守卫
        ├── stores/auth.js
        ├── api/request.js            ← axios 拦截器
        └── views/
            ├── Login.vue
            ├── Layout.vue
            └── Dashboard.vue
```

> 业务端前端（`frontend-web/`）由 PM / Member 负责人创建。**后端是同一份**（`backend/`），三端共用。

---

## 5. 与业务端的接口边界

| 路径前缀 | 归属 | Admin 权限 |
|---|---|---|
| `/api/auth/**` | 共享 | 完全复用 |
| `/api/admin/**` | **本端** | 读写自己的管理数据 |
| `/api/projects/**` 等业务路径 | 业务端 | **仅 GET 放行，写请求一律 403** |

§5 第三行是跨端依赖，需与业务端确认拦截器的放行规则。

---

## 6. 待定

| # | 事项 |
|---|---|
| 1 | A2 审计 AOP 切面由谁实现（阻塞 AD6） |
| 2 | 业务端看板/甘特组件是否支持 `readonly` 模式（阻塞 AD7） |
| 3 | PM 端"申请加人"入口的接口路径（阻塞 AD8.1） |
