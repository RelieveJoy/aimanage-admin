-- ============================================================
--  爱管理 · 表结构（唯一真源）
--  形态：单企业、多项目（见 ARCHITECTURE.md §0）
--  注意：所有表均无 tenant_id —— 一套部署服务一家公司
-- ============================================================

CREATE DATABASE IF NOT EXISTS aimanage
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE aimanage;

-- ------------------------------------------------------------
-- 1. 用户（三端共用，一张表）
-- ------------------------------------------------------------
-- 表名用 sys_user 而非 user：user 在 MySQL 中是保留字，避免到处加反引号
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)  NOT NULL                COMMENT '登录名，全局唯一',
    `password`      VARCHAR(100) NOT NULL                COMMENT 'BCrypt 哈希，禁止存明文',
    `name`          VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
    `role`          VARCHAR(10)  NOT NULL                COMMENT 'ADMIN / PM / MEMBER（三值枚举，代码内置）',
    `dept_id`       BIGINT       NULL                    COMMENT '所属部门',
    `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 / 0 停用',
    `token_version` INT          NOT NULL DEFAULT 0      COMMENT '改密码时 +1，使旧 token 失效',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 部门（两层：公司 → 部门）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(50) NOT NULL,
    `parent_id`  BIGINT      NOT NULL DEFAULT 0     COMMENT '0 表示公司根节点',
    `sort`       INT         NOT NULL DEFAULT 0,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表（树根即本公司）';

-- ------------------------------------------------------------
-- 3. 项目（单企业多项目）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(100) NOT NULL,
    `code`        VARCHAR(30)  NULL              COMMENT '项目编号，可自动生成',
    `description` VARCHAR(500) NULL,
    `pm_id`       BIGINT       NULL              COMMENT '项目经理，必须是 role=PM 的用户',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 进行中 / 0 已归档',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_pm` (`pm_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ------------------------------------------------------------
-- 4. 项目成员  ★ 成员挂在项目下，不挂在 PM 名下（已确认）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `project_id`      BIGINT      NOT NULL,
    `user_id`         BIGINT      NOT NULL,
    `role_in_project` VARCHAR(10) NOT NULL DEFAULT 'MEMBER' COMMENT 'PM / MEMBER',
    `joined_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user` (`project_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员关系。增删由 A2 审计切面自动留痕，无需单独埋点';

-- ------------------------------------------------------------
-- 5. 审计账本  ★ 本端核心价值的数据源（对应方案 A2）
--    写入方：后端 AOP 切面（跨端共享，非 Admin 端实现）
--    读取方：Admin 端 AD6 检索页
--    本表无 UPDATE / DELETE 接口，Admin 亦不可改
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id`     BIGINT       NOT NULL             COMMENT '操作人',
    `operator_role`   VARCHAR(10)  NOT NULL             COMMENT '操作时角色快照',
    `project_id`      BIGINT       NULL                 COMMENT '所属项目，便于按项目检索',
    `target_type`     VARCHAR(30)  NOT NULL             COMMENT 'TASK / REQUIREMENT / PROJECT / PROJECT_MEMBER',
    `target_id`       BIGINT       NOT NULL,
    `target_name`     VARCHAR(200) NULL                 COMMENT '冗余对象名，避免检索时联表',
    `field`           VARCHAR(50)  NULL                 COMMENT '变更字段；新增/删除时为 NULL',
    `before_value`    TEXT         NULL                 COMMENT '变更前值（方案强调：竞品缺的就是这一列）',
    `after_value`     TEXT         NULL                 COMMENT '变更后值',
    `before_snapshot` JSON         NULL                 COMMENT '整对象快照，供详情抽屉对比',
    `after_snapshot`  JSON         NULL,
    `action`          VARCHAR(10)  NOT NULL DEFAULT 'UPDATE' COMMENT 'CREATE / UPDATE / DELETE',
    `remark`          VARCHAR(500) NULL                 COMMENT '备注，如"客户方王总电话确认"',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project_time` (`project_id`, `created_at`),
    KEY `idx_operator_time` (`operator_id`, `created_at`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_field` (`field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全链路审计账本（只增不改不删）';

-- ------------------------------------------------------------
-- 6. 加人申请（方案 B：本期唯一的事件源）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `join_request`;
CREATE TABLE `join_request` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `project_id`     BIGINT      NOT NULL,
    `applicant_id`   BIGINT      NOT NULL             COMMENT '发起人（PM）',
    `target_user_id` BIGINT      NOT NULL             COMMENT '被申请加入的人',
    `reason`         VARCHAR(500) NULL,
    `status`         VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
    `reviewer_id`    BIGINT      NULL                 COMMENT '审批人（Admin）',
    `review_comment` VARCHAR(500) NULL,
    `reviewed_at`    DATETIME    NULL,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目加人申请';

-- ------------------------------------------------------------
-- 7. 通知（Administrator 只收面向自己的，不做全量订阅）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `receiver_id` BIGINT       NOT NULL             COMMENT '接收人；Admin 端只查自己的',
    `type`        VARCHAR(30)  NOT NULL             COMMENT 'JOIN_REQUEST 等',
    `title`       VARCHAR(200) NOT NULL,
    `payload`     JSON         NULL                 COMMENT '跳转所需上下文，如 {requestId, projectId}',
    `read_at`     DATETIME     NULL                 COMMENT 'NULL 表示未读',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_receiver_unread` (`receiver_id`, `read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';
