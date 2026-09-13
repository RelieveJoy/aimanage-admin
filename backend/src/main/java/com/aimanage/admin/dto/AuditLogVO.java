package com.aimanage.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计账本条目。
 *
 * <p>这个结构的字段顺序就是"责任判定证据链"要回答的问题：
 * <b>谁</b>（operatorName）<b>在什么时候</b>（createdAt）<b>在哪个项目</b>（projectName）
 * <b>对哪个对象</b>（targetName）<b>的哪个字段</b>（field）
 * <b>从什么</b>（beforeValue）<b>改成了什么</b>（afterValue）<b>为什么</b>（remark）。
 *
 * <p>AD5.3 的「成员变更历史」与 AD6 的「全局审计检索」共用本结构。
 */
@Data
public class AuditLogVO {

    private Long id;

    private Long operatorId;
    private String operatorName;
    private String operatorRole;

    private Long projectId;
    private String projectName;

    private String targetType;
    private Long targetId;
    private String targetName;

    private String field;
    private String beforeValue;
    private String afterValue;

    private String action;
    private String remark;

    private LocalDateTime createdAt;
}
