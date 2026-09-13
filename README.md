# 爱管理 · 管理员端

> 课程项目《爱管理（iManage）》—— 管理员端（Epic AD）
> 独立前端 + 复用后端，单企业多项目形态

---

## 这是什么

**爱管理**是一个面向企业的项目管理平台。整个系统分三个端：

| 端 | 角色 | 职责 |
|---|---|---|
| 工人端 | Member | 领任务、改状态、评论 |
| 项目经理端 | PM | 管项目、排期、分配任务 |
| **管理员端（本项目）** | **Admin** | **管人、管组织、管证据链 —— 对业务数据只看不写** |

管理员是**购买了这套软件的那家公司的管理员**。因此本系统是**单企业、多项目**形态：
一套部署服务一家公司，一个管理员管这家公司的所有项目经理和成员。

> ⚠️ 不要按多租户 SaaS 设计。所有表都**没有** `tenant_id`，也没有企业切换功能。

---

## 快速开始（Docker，推荐）

**唯一前置条件：装好 Docker Desktop。**

```bash
git clone <本仓库地址>
cd 软件项目管理

docker compose up -d --build
```

启动后：

| 地址 | 说明 |
|---|---|
| **http://localhost:8081** | 管理端界面 ← 从这里进 |
| http://localhost:8080 | 后端接口 |
| 127.0.0.1:3307 | MySQL（Navicat / DataGrip 可连） |

**默认管理员账号：**

```
用户名：admin
密码：  123456
```

> 首次登录后请立即通过「用户管理 → 重置密码」修改。

### 常用命令

```bash
docker compose logs -f backend    # 看后端日志
docker compose ps                 # 看容器状态
docker compose down               # 停止（数据保留）
docker compose down -v            # 停止并清空数据库（重置）
```

### 数据库只初始化一次

`db/schema.sql` 只在**数据卷为空的首次启动**时执行。后续改表结构需要手动重建：

```bash
docker compose down -v && docker compose up -d --build
```

---

## 本地开发（不用 Docker）

需要：JDK 17+、Maven 3.9+、Node 20+、MySQL 8.0

**1. 建库**

```bash
mysql -uroot -p < backend/src/main/resources/db/schema.sql
```

**2. 配数据库密码**

编辑 `backend/src/main/resources/application.yml`，把 `DB_PASSWORD` 的默认值改成你的密码：

```yaml
password: ${DB_PASSWORD:你的密码}
```

**3. 起后端**

```bash
cd backend
mvn spring-boot:run
```

首次启动会自动创建 `admin / 123456`。

**4. 起前端**

```bash
cd frontend-admin
npm install
npm run dev
```

打开 http://localhost:5173 —— vite 已配好 `/api` 代理到 8080，前端代码里一律写相对路径。

---

## 目录结构

```
软件项目管理/
├── docker-compose.yml              # 一键启动 MySQL + 后端 + 前端
├── README.md
├── admin端_故事地图与任务分解.md      # ★ 需求侧：Epic AD 的故事与 Sprint 切片
│
├── docs/admin/
│   ├── ARCHITECTURE.md             # ★ 技术契约：选型、模块、鉴权数据流
│   └── API_SPEC.md                 # ★ 接口契约：路径、请求响应、错误码
│
├── backend/                        # 后端（三端共用这一份）
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/aimanage/
│       │   ├── common/             # 统一响应体、异常处理
│       │   ├── security/           # ★ 鉴权：JWT + 拦截器 + @RequireRole
│       │   ├── config/             # 拦截器注册、跨域、种子数据
│       │   ├── auth/               # 登录（三端共用）
│       │   ├── entity/  mapper/
│       │   └── admin/              # ★ 管理端专属接口 /api/admin/**
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql       # ★ 表结构唯一真源
│
└── frontend-admin/                 # 管理端前端（独立工程）
    ├── Dockerfile  nginx.conf
    └── src/
        ├── api/                    # axios 封装 + 接口定义
        ├── stores/auth.js          # 登录态
        ├── router/index.js         # ★ 路由守卫
        └── views/                  # 登录、布局、概览、用户管理
```

---

## 技术选型

| 层 | 选型 | 为什么 |
|---|---|---|
| 后端 | Spring Boot 3.2 + Java 17 | 方案要求 AOP 切面，Spring 原生支持 |
| ORM | MyBatis-Plus | 管理端 CRUD 密集 |
| 认证 | JWT (jjwt) | 无状态，三端共用 |
| 鉴权 | 自建拦截器 + `@RequireRole` | **不用 Spring Security** —— 课设场景太重 |
| 前端 | Vue 3 + Vite + Element Plus | 管理端全是表格/表单/树 |
| 数据库 | MySQL 8.0 | — |

---

## 鉴权设计（重要）

```
前端路由守卫   →  只是体验层，能挡住误入界面，挡不住直接调接口
                        ↓
后端 AuthInterceptor  →  ★ 真正的安全边界
   1. 无 token / 无效 / 过期  →  401
   2. 路径以 /api/admin/ 开头且 role != ADMIN  →  403
```

**任何新增的 `/api/admin/**` 接口自动获得管理员保护**，不需要逐个加注解。

对应的验收测试（方案 DoD 第 2 条）：

```bash
# 用 Member 的 token 打管理端接口，必须返回 403
curl http://localhost:8080/api/admin/users -H "Authorization: Bearer <member-token>"
# => {"code":403,"msg":"无权访问管理端接口"}
```

---

## 当前进度

### 已完成（Sprint 1 部分）

| 编号 | 功能 | 状态 |
|---|---|---|
| AD1 | 管理员登录与入口隔离 | ✅ 后端 + 前端 |
| AD2 | 用户账号管理（建 PM / 建 Member / 启停 / 重置密码） | ✅ 后端 + 前端 |
| — | Docker 一键部署 | ✅ |
| — | 概览页（统计 + 进度） | ✅ |

### 待开发

| 编号 | 功能 | Sprint |
|---|---|---|
| AD3 | 组织架构（部门树 + 成员归属） | Sprint 1 |
| AD4 | 项目管理（建项目 / 指定 PM / 归档） | Sprint 2 |
| AD5 | 项目成员与成员变化 | Sprint 2 |
| AD7 | 只读观测（看板 / 甘特） | Sprint 2 |
| AD6 | 全局审计账本检索 | Sprint 3 |
| AD8 | 加人申请审批 + 通知中心 | Sprint 3 |

**侧边菜单里灰色的项就是这些未完成的故事** —— 有意摆出来，让功能边界可见。

---

## 已知依赖（需要和其他端对齐）

| # | 依赖 | 说明 |
|---|---|---|
| 1 | **审计 AOP 切面** | `audit_log` 的**写入方**属共享后端，尚无归属。AD6 完全依赖它 |
| 2 | 只读观测 | 业务端看板/甘特组件需支持 `readonly` 模式 |
| 3 | 加人申请 | PM 端需提供「申请加人」入口 |

---

## 文档索引

| 文档 | 内容 |
|---|---|
| [admin端_故事地图与任务分解.md](admin端_故事地图与任务分解.md) | 需求侧：Epic AD 九个故事、Sprint 切片、跨端依赖、验收标准、演示脚本 |
| [docs/admin/ARCHITECTURE.md](docs/admin/ARCHITECTURE.md) | 技术侧：选型理由、模块划分、鉴权数据流、目录结构 |
| [docs/admin/API_SPEC.md](docs/admin/API_SPEC.md) | 接口契约：统一响应体、错误码、全部接口定义 |
| [backend/src/main/resources/db/schema.sql](backend/src/main/resources/db/schema.sql) | 表结构唯一真源（7 张表） |

---

## 排错

| 现象 | 原因 / 解决 |
|---|---|
| `project name must not be empty` | 目录名是中文，Compose 推导不出项目名。已在 `docker-compose.yml` 里用 `name: aimanage` 固定 |
| `Unsupported character encoding 'utf8mb4'` | JDBC 的 `characterEncoding` 要填 Java 字符集名 `UTF-8`，不是 MySQL 的 `utf8mb4` |
| 3306 端口被占 | 宿主机已有本机 MySQL。容器映射到 **3307**，连容器数据库请用 3307 |
| 前端页面空白 / 刷新子页 404 | nginx 已配 `try_files ... /index.html` 回落；若本地开发请确认 vite 的 history 模式配置 |
| 拉基础镜像失败 | 网络抖动，重试 `docker compose up -d --build` 即可 |
