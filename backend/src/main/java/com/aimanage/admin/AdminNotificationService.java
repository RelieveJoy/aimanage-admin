package com.aimanage.admin;

import com.aimanage.admin.dto.NotificationVO;
import com.aimanage.common.BizException;
import com.aimanage.entity.Notification;
import com.aimanage.entity.User;
import com.aimanage.mapper.NotificationMapper;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.RoleEnum;
import com.aimanage.security.UserContext;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知：面向管理员的收件与已读标记，以及给其他角色发通知的工具方法。
 *
 * <p><b>只收面向自己的通知</b>（{@code receiver_id = 当前登录用户}），不做全量订阅。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    // ---------------------------------------------------------------- 查询（当前用户）

    /** 当前用户的通知列表 */
    public List<NotificationVO> list(boolean unreadOnly) {
        Long me = UserContext.currentUserId();
        var w = Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getReceiverId, me)
                .orderByDesc(Notification::getCreatedAt);
        if (unreadOnly) {
            w.isNull(Notification::getReadAt);
        }
        return notificationMapper.selectList(w).stream().map(NotificationVO::from).toList();
    }

    public long unreadCount() {
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getReceiverId, UserContext.currentUserId())
                .isNull(Notification::getReadAt));
        return count == null ? 0 : count;
    }

    @Transactional
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) {
            throw BizException.notFound("通知不存在");
        }
        // 只能标记自己的通知 —— 否则就是个越权读的漏洞
        if (!n.getReceiverId().equals(UserContext.currentUserId())) {
            throw BizException.forbidden("无权操作他人的通知");
        }
        if (n.isUnread()) {
            n.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    @Transactional
    public void markAllRead() {
        Long me = UserContext.currentUserId();
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getReceiverId, me)
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    // ---------------------------------------------------------------- 发送

    /**
     * 给所有启用中的管理员发通知。
     *
     * <p>用于「PM 提交加人申请」这个场景 —— 管理员是收件人。
     */
    public void notifyAllAdmins(String type, String title, String payloadJson) {
        List<User> admins = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, RoleEnum.ADMIN.name())
                .eq(User::getStatus, 1));
        for (User admin : admins) {
            send(admin.getId(), type, title, payloadJson);
        }
    }

    /**
     * 给指定用户发通知。
     *
     * <p>用 try/catch 包住：通知发不出去不应该让业务失败，
     * 与审计切面同一条铁律。
     */
    public void send(Long receiverId, String type, String title, String payloadJson) {
        try {
            Notification n = new Notification();
            n.setReceiverId(receiverId);
            n.setType(type);
            n.setTitle(title);
            n.setPayload(payloadJson);
            notificationMapper.insert(n);
        } catch (Exception e) {
            log.error("发送通知失败 receiverId={} type={}", receiverId, type, e);
        }
    }
}
