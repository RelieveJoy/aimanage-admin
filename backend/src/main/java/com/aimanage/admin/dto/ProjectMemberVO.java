package com.aimanage.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员。含系统级角色与项目内角色两个维度。
 */
@Data
public class ProjectMemberVO {

    private Long userId;
    private String username;
    private String name;

    /** 系统级角色：ADMIN / PM / MEMBER */
    private String systemRole;

    /** 项目内角色：PM / MEMBER */
    private String roleInProject;

    private String deptName;

    /** 用户是否被停用 —— 停用的人仍在项目里，但要标出来 */
    private Integer status;

    private LocalDateTime joinedAt;
}
