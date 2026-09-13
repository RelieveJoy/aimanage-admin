# Admin 端 · 接口契约

> 表结构唯一真源：`backend/src/main/resources/db/schema.sql`（本文件不重复 DDL）
> 状态：Sprint 1 接口已冻结；Sprint 2/3 接口为草案，实现前可调整。

---

## 0. 通用约定

### 0.1 统一响应体

```json
{ "code": 0, "msg": "ok", "data": {} }
```

| code | 含义 |
|---|---|
| `0` | 成功 |
| `400` | 参数校验失败 |
| `401` | 未登录 / token 无效或过期 |
| `403` | 已登录但无权限（★ 越权测试的判定依据） |
| `404` | 资源不存在 |
| `409` | 业务冲突（如用户名重复、部门非空不可删） |
| `500` | 服务端异常 |

> HTTP 状态码与 `code` 保持一致，便于前端拦截器统一处理。

### 0.2 分页响应

```json
{ "code": 0, "msg": "ok",
  "data": { "total": 137, "page": 1, "size": 20, "records": [] } }
```

### 0.3 认证头

除 `/api/auth/login` 外，所有接口需携带：

```
Authorization: Bearer <token>
```

---

## 1. 认证（三端共用，Admin 端完全复用）

### POST `/api/auth/login`

请求：
```json
{ "username": "admin", "password": "123456" }
```

响应：
```json
{ "code": 0, "msg": "ok",
  "data": {
    "token": "eyJhbGciOi...",
    "user": { "id": 1, "username": "admin", "name": "系统管理员", "role": "ADMIN" }
  } }
```

**异常**
| 场景 | code | msg |
|---|---|---|
| 用户名不存在 / 密码错误 | `401` | **"用户名或密码错误"**（两者同一提示，不泄露账号是否存在） |
| 账号已停用 | `403` | "账号已停用，请联系管理员" |

### POST `/api/auth/logout`
无请求体。前端清除本地 token 即可，服务端直接返回成功。

### GET `/api/auth/me`
返回当前登录用户，用于刷新页面后恢复前端状态。
```json
{ "code": 0, "msg": "ok",
  "data": { "id": 1, "username": "admin", "name": "系统管理员", "role": "ADMIN" } }
```

---

## 2. 用户管理（Sprint 1 · Must）

### GET `/api/admin/users`

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 模糊匹配 username 或 name |
| role | string | 否 | `PM` / `MEMBER` |
| deptId | long | 否 | 部门筛选 |
| status | int | 否 | `1` 启用 / `0` 停用 |
| page | int | 否 | 默认 1 |
| size | int | 否 | 默认 20 |

响应 `records[]` 元素：
```json
{ "id": 2, "username": "zhangsan", "name": "张三",
  "role": "PM", "deptId": 10, "deptName": "研发部",
  "status": 1, "createdAt": "2026-09-13 10:00:00" }
```

> 响应体**不含 password 字段**。

### POST `/api/admin/users`

```json
{ "username": "lisi", "name": "李四", "password": "123456",
  "role": "MEMBER", "deptId": 10 }
```

**约束**：`role` 只接受 `PM` / `MEMBER`，传 `ADMIN` 返回 `400`。

**异常**
| 场景 | code |
|---|---|
| 用户名已存在 | `409` "用户名已存在" |
| role 为 ADMIN 或非法值 | `400` "角色只能是 PM 或 MEMBER" |
| 用户名/密码为空 | `400` |

### PATCH `/api/admin/users/{id}`

可改字段：`name` / `role` / `deptId` / `status`。字段缺省表示不修改。

**异常**
| 场景 | code |
|---|---|
| 改为 ADMIN | `400` |
| 停用自己 | `409` "不能停用当前登录账号" |
| 停用系统内最后一个启用的 ADMIN | `409` "系统必须保留至少一个启用的管理员" |

### POST `/api/admin/users/{id}/reset-password`

```json
{ "newPassword": "123456" }
```
成功后该用户**已有 token 在下次请求时失效**（实现方式：`user` 表加 `token_version`，改密码时 +1，JWT 内携带并比对）。

---

## 3. 组织架构（Sprint 1 · Must）

> **树的形状是两层的**：公司（根，`parentId = 0`）→ 部门。
> 在部门下再建子部门会返回 `400`——单企业课设场景下三层无真实收益，有意不做。

### GET `/api/admin/departments`
返回完整部门树。

```json
{ "code": 0, "msg": "ok", "data": [
  { "id": 1, "name": "022 科技有限公司", "parentId": 0, "sort": 0,
    "memberCount": 0, "isRoot": true,
    "children": [
      { "id": 2, "name": "研发部", "parentId": 1, "sort": 1,
        "memberCount": 1, "isRoot": false, "children": [] }
    ] } ] }
```

`memberCount` 是该部门**直属**人数（树只有两层，无子部门可叠加）。

### POST `/api/admin/departments`
```json
{ "name": "测试部", "parentId": 1, "sort": 2 }
```

`parentId` 省略时默认挂到公司根节点。

**异常**
| 场景 | code | msg |
|---|---|---|
| `parentId` 指向的不是公司根节点 | `400` | "组织架构只支持两层，不能在部门下再建子部门" |
| `parentId` 不存在 | `404` | "上级部门不存在" |

### PATCH `/api/admin/departments/{id}`
可改 `name` / `sort`。**不支持改 `parentId`** —— 拖拽改层级属过度设计。

### DELETE `/api/admin/departments/{id}`

**异常**
| 场景 | code | msg |
|---|---|---|
| 删除公司根节点 | `409` | "公司根节点不能删除" |
| 部门下有子部门 | `409` | "该部门下仍有子部门，无法删除" |
| 部门下有成员 | `409` | "该部门下仍有成员，无法删除" |

### 调整成员部门
复用 `PATCH /api/admin/users/{id}`，传 `deptId`：

```json
{ "deptId": 2 }
```

> ⚠️ **`deptId = 0` 表示"不分配"（清空部门）。**
> 因为 PATCH 语义下 `null` 代表"不修改"，需要一个哨兵值来表达"清空"。
> 实现上不能只 `setDeptId(null)` —— MyBatis-Plus 的 `updateById` 默认忽略 null 字段，
> 置空必须走 `UpdateWrapper` 显式 `set`。

| 传入值 | 效果 |
|---|---|
| 省略 `deptId` | 不修改部门 |
| `deptId: 0` | 清空部门（变为"未分配"） |
| `deptId: <真实 ID>` | 调整到该部门；不存在则 `400` |

---

## 4. 项目管理（Sprint 2 · Must）✅ 已实现

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/projects` | 全公司项目；`keyword`（名称或编号）/ `status` / `page` / `size` |
| GET | `/api/admin/projects/{id}` | 项目详情 |
| POST | `/api/admin/projects` | `{name, description, pmId}` |
| PATCH | `/api/admin/projects/{id}` | 改名 / 改描述 / 换 PM / 归档 |

响应元素：

```json
{ "id": 1, "name": "爱管理项目", "code": "PRJ-001",
  "description": "第三代项目管理工具",
  "pmId": 3, "pmName": "张三",
  "status": 1, "memberCount": 2,
  "createdAt": "2026-09-13 15:43:58" }
```

**`code` 由后端自动生成**，格式 `PRJ-001`（取已有最大编号 +1），前端不传。

**约束**
| 场景 | code | msg |
|---|---|---|
| `pmId` 对应用户不是 PM 角色 | `400` | "项目经理必须是「项目经理」角色的用户" |
| `pmId` 对应用户已停用 | `400` | "该账号已停用，不能担任项目经理" |
| 项目不存在 | `404` | "项目不存在" |

**排序**：进行中优先，同状态内按创建时间倒序。

**归档语义**（`status: 0`）：数据全部保留，仅从默认视图隐藏；成员端不再看到该项目。

---

## 5. 项目成员（Sprint 2 · Must）✅ 已实现

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/projects/{id}/members` | 成员列表 |
| POST | `/api/admin/projects/{id}/members` | `{userId}` — Admin 直接加人，无需审批 |
| DELETE | `/api/admin/projects/{id}/members/{userId}` | 移出项目 |
| GET | `/api/admin/projects/{id}/member-history` | **成员变更历史**（AD5.3） |

成员元素：

```json
{ "userId": 2, "username": "test_member", "name": "测试成员",
  "systemRole": "MEMBER",        // 系统级角色
  "roleInProject": "MEMBER",     // 项目内角色
  "deptName": "研发部",
  "status": 1, "joinedAt": "2026-09-13 15:44:00" }
```

**约束**
| 场景 | code | msg |
|---|---|---|
| 用户不存在 | `404` | "用户不存在" |
| 用户已停用 | `400` | "该账号已停用，无法加入项目" |
| 用户已在项目中 | `409` | "该用户已在项目中" |
| 移出项目经理本人 | `409` | "不能把项目经理移出项目，请先更换项目经理" |

**两个行为约定**
1. **新建项目时 PM 自动成为项目成员**（`roleInProject = PM`），否则成员列表里看不到 PM 自己。
2. **更换 PM 时，原 PM 降为普通成员**而非被踢出 —— 他可能仍在项目里干活。

**`member-history` 不查单独的埋点表**，直接读 `audit_log` 中 `target_type = 'PROJECT_MEMBER'` 的记录。响应结构同 §7。

---

## 6. 只读观测（Sprint 2 · Must）⏸ 未实现

Admin 端**不新增接口**，复用业务端既有 GET：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/projects/{id}/kanban` | 看板数据，Admin 只读 |
| GET | `/api/projects/{id}/gantt` | 甘特数据，Admin 只读 |

**约束**：业务端所有写接口（POST/PATCH/DELETE）必须对 `role=ADMIN` 返回 `403`。

> ⏸ **阻塞中**：依赖业务端提供支持 `readonly` 的看板/甘特组件。

---

## 7. 审计账本检索（Sprint 3 · Must）✅ 已实现

### GET `/api/admin/audits`

| 参数 | 类型 | 说明 |
|---|---|---|
| projectId | long | 按项目 |
| operatorId | long | 按操作人 |
| targetType | string | `PROJECT` / `PROJECT_MEMBER` / `USER` / `DEPARTMENT` |
| field | string | 字段名，如 `角色`、`项目名称` |
| startTime / endTime | string | `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| keyword | string | 模糊匹配 `targetName` / `remark` / 前后值 |
| page / size | int | 默认 1 / 20 |

响应元素：

```json
{ "id": 9001,
  "operatorId": 1, "operatorName": "系统管理员", "operatorRole": "ADMIN",
  "projectId": 1, "projectName": "爱管理项目",
  "targetType": "PROJECT_MEMBER", "targetId": 2, "targetName": "测试成员",
  "field": "项目成员",
  "beforeValue": null, "afterValue": "测试成员",
  "action": "CREATE",
  "remark": "加入项目「爱管理项目」",
  "createdAt": "2026-09-13 15:44:00" }
```

> **时间边界处理**：`endTime` 只给日期（如 `2026-09-13`）时自动补到当天 `23:59:59`，
> 否则用户会困惑"今天发生的变更怎么筛不出来"。

### GET `/api/admin/audits/{id}`
单条详情，结构同上。

> **只读硬约束**：`audit_log` 未暴露任何写接口，Admin 亦不可修改或删除，对应方案的"不可篡改"。

---

## 8. 审计账本的写入方（共享后端，不属于本端）

`audit_log` 的数据**不由 Admin 端写入**，而由审计切面在业务写操作发生时自动记录：

| 组件 | 位置 | 职责 |
|---|---|---|
| `@Auditable` | `com.aimanage.audit` | 标注需要审计的方法 |
| `AuditContext` | 同上 | 业务方法向切面补充"改了什么" |
| `AuditAspect` | 同上 | 统一记录操作人、时间、落库，异常不阻塞业务 |

业务端新增写方法时，加上 `@Auditable` 注解即可自动纳入审计。
详见 `docs/admin/ARCHITECTURE.md` §7。

---

## 8. 申请审批与通知（Sprint 3）— 草案

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/requests` | 待审批列表；`?status=PENDING` |
| POST | `/api/admin/requests/{id}/approve` | `{comment}` → 写入 `project_member` + 写审计 |
| POST | `/api/admin/requests/{id}/reject` | `{comment}` **必填** |
| GET | `/api/admin/notifications` | `?unread=true`，**仅返回 receiver_id = 当前管理员** |
| POST | `/api/admin/notifications/{id}/read` | 标记已读 |
| POST | `/api/admin/notifications/read-all` | 全部已读 |

> **跨端依赖**：`POST /api/requests`（PM 提交加人申请）由 PM 端实现，Admin 端只消费。
