package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目成员关系，对应表 {@code project_member}。
 *
 * <p><b>成员挂在项目下</b>（已与需求方确认），不挂在 PM 名下。
 * 一个人可以同时属于多个项目。
 *
 * <p>本表的增删由审计切面自动留痕，因此 AD5.3「成员变更历史」
 * 不需要单独的埋点表 —— 直接查 audit_log 即可。
 */
@Data
@TableName("project_member")
public class ProjectMember {

    /** 项目经理在项目内的角色标识 */
    public static final String ROLE_PM = "PM";

    /** 普通成员在项目内的角色标识 */
    public static final String ROLE_MEMBER = "MEMBER";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long userId;

    /** 在<b>本项目内</b>的角色，与用户的系统级 role 分开 */
    private String roleInProject;

    private LocalDateTime joinedAt;
}
