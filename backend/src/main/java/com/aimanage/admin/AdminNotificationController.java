package com.aimanage.admin;

import com.aimanage.admin.dto.NotificationVO;
import com.aimanage.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端 · 通知中心。
 *
 * <p>只返回<b>发给当前登录管理员</b>的通知，不做全量订阅。
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    @GetMapping
    public R<List<NotificationVO>> list(@RequestParam(defaultValue = "false") boolean unread) {
        return R.ok(notificationService.list(unread));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        return R.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead();
        return R.ok();
    }
}
