package com.aimanage.admin;

import com.aimanage.admin.dto.JoinRequestVO;
import com.aimanage.admin.dto.ReviewRequest;
import com.aimanage.common.PageResult;
import com.aimanage.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端 · 加人申请审批（AD8）。
 */
@RestController
@RequestMapping("/api/admin/requests")
@RequiredArgsConstructor
public class AdminRequestController {

    private final AdminRequestService adminRequestService;

    @GetMapping
    public R<PageResult<JoinRequestVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(adminRequestService.page(status, page, size));
    }

    /** 待审批数量，用于菜单角标 */
    @GetMapping("/pending-count")
    public R<Map<String, Long>> pendingCount() {
        return R.ok(Map.of("count", adminRequestService.pendingCount()));
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id,
                           @Valid @RequestBody(required = false) ReviewRequest req) {
        adminRequestService.approve(id, req == null ? new ReviewRequest() : req);
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id,
                          @Valid @RequestBody ReviewRequest req) {
        adminRequestService.reject(id, req);
        return R.ok();
    }
}
