package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计账本，对应表 {@code audit_log}（方案 A2）。
 *
 * <p><b>只增不改不删</b> —— 本实体没有任何更新或删除的调用方。
 * 写入方是 {@code audit} 包下的 AOP 切面；Admin 端只做读取。
 */
@Data
@TableName("audit_log")
public class AuditLog {

    /** 目标对象类型 */
    public static final String TARGET_PROJECT = "PROJECT";
    public static final String TARGET_PROJECT_MEMBER = "PROJECT_MEMBER";
    public static final String TARGET_USER = "USER";
    public static final String TARGET_DEPARTMENT = "DEPARTMENT";
    public static final String TARGET_JOIN_REQUEST = "JOIN_REQUEST";

    /** 动作 */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    /** 操作时的角色快照 —— 用户之后改角色，历史记录不应跟着变 */
    private String operatorRole;

    /** 所属项目，便于"按项目检索" */
    private Long projectId;

    private String targetType;
    private Long targetId;
    private String targetName;

    private String field;

    /** 方案反复强调：竞品缺的就是这两列 */
    private String beforeValue;
    private String afterValue;

    private String action;

    private String remark;

    private LocalDateTime createdAt;
}
