package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目加人申请，对应表 {@code join_request}。
 *
 * <p>本期唯一的申请类型（方案 B 的取舍：只做一个事件源）。
 * 流程：PM 提交 → Admin 审批 → 通过则写入 {@code project_member}。
 *
 * <p>为什么不让 PM 直接加人：这样"组织架构 + 项目成员"完全归属管理员，
 * 与"明确每个项目的 PM 和成员是谁"这条需求一致。
 */
@Data
@TableName("join_request")
public class JoinRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    /** 发起人（项目经理） */
    private Long applicantId;

    /** 被申请加入的人 */
    private Long targetUserId;

    private String reason;

    private String status;

    /** 审批人（管理员） */
    private Long reviewerId;

    private String reviewComment;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
