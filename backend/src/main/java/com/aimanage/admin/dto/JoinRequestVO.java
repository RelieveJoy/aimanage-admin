package com.aimanage.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 加人申请。字段围绕审批时管理员要判断的三件事组织：
 * <b>谁申请</b>（applicantName）、<b>给哪个项目</b>（projectName）、
 * <b>加谁</b>（targetName + targetDeptName + targetAlreadyInProject）。
 */
@Data
public class JoinRequestVO {

    private Long id;

    private Long projectId;
    private String projectName;

    private Long applicantId;
    private String applicantName;
    /** 申请人在该项目内的角色 */
    private String applicantRoleInProject;

    private Long targetUserId;
    private String targetName;
    private String targetUsername;
    private String targetDeptName;
    /** 目标用户是否已在项目中 —— 审批前先让管理员看到，避免批准了才发现重复 */
    private Boolean targetAlreadyInProject;

    private String reason;

    private String status;

    private Long reviewerId;
    private String reviewerName;
    private String reviewComment;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
