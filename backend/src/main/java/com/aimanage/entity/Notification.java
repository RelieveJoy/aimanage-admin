package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知，对应表 {@code notification}。
 *
 * <p><b>管理员只收面向自己的通知</b>（{@code receiver_id = 当前管理员}），
 * 不做全量订阅 —— 这是有意的范围收敛。
 */
@Data
@TableName("notification")
public class Notification {

    /** 收到一条加人申请，待审批 */
    public static final String TYPE_JOIN_REQUEST = "JOIN_REQUEST";

    /** 申请被批准 */
    public static final String TYPE_REQUEST_APPROVED = "REQUEST_APPROVED";

    /** 申请被拒绝 */
    public static final String TYPE_REQUEST_REJECTED = "REQUEST_REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;

    private String type;

    private String title;

    /** 跳转所需上下文，JSON 字符串 */
    private String payload;

    /** null 表示未读 */
    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    public boolean isUnread() {
        return readAt == null;
    }
}
