package com.aimanage.admin;

import com.aimanage.admin.dto.AuditLogVO;
import com.aimanage.common.PageResult;
import com.aimanage.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 全局审计账本检索（AD6）—— 本端的核心价值展示面。
 *
 * <p><b>只读接口</b>。审计账本没有暴露任何写接口，Admin 自己也不能改或删，
 * 这对应方案说的"不可篡改"。
 */
@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public R<PageResult<AuditLogVO>> search(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(adminAuditService.search(projectId, operatorId, targetType, field,
                startTime, endTime, keyword, page, size));
    }

    @GetMapping("/{id}")
    public R<AuditLogVO> detail(@PathVariable Long id) {
        return R.ok(adminAuditService.detail(id));
    }
}
