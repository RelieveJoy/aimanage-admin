package com.aimanage.admin.dto;

import com.aimanage.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private String type;
    private String title;

    /** 前端据此跳转，如 {"requestId":3} */
    private String payload;

    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationVO from(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setType(n.getType());
        vo.setTitle(n.getTitle());
        vo.setPayload(n.getPayload());
        vo.setRead(n.isUnread() ? Boolean.FALSE : Boolean.TRUE);
        vo.setReadAt(n.getReadAt());
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
