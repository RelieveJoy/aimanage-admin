package com.aimanage.admin;

import com.aimanage.admin.dto.AddMemberRequest;
import com.aimanage.admin.dto.AuditLogVO;
import com.aimanage.admin.dto.CreateProjectRequest;
import com.aimanage.admin.dto.ProjectMemberVO;
import com.aimanage.admin.dto.ProjectVO;
import com.aimanage.admin.dto.UpdateProjectRequest;
import com.aimanage.common.PageResult;
import com.aimanage.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 项目管理与项目成员（AD4 / AD5）。
 *
 * <p>路径以 {@code /api/admin/} 开头，自动受 AuthInterceptor 的 ADMIN 保护。
 */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final AdminProjectService adminProjectService;
    private final AdminAuditService adminAuditService;

    // ---------------- 项目 ----------------

    @GetMapping
    public R<PageResult<ProjectVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(adminProjectService.page(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public R<ProjectVO> detail(@PathVariable Long id) {
        return R.ok(adminProjectService.detail(id));
    }

    @PostMapping
    public R<ProjectVO> create(@Valid @RequestBody CreateProjectRequest req) {
        return R.ok(adminProjectService.create(req));
    }

    @PatchMapping("/{id}")
    public R<ProjectVO> update(@PathVariable Long id,
                               @Valid @RequestBody UpdateProjectRequest req) {
        return R.ok(adminProjectService.update(id, req));
    }

    // ---------------- 成员 ----------------

    @GetMapping("/{id}/members")
    public R<List<ProjectMemberVO>> members(@PathVariable Long id) {
        return R.ok(adminProjectService.members(id));
    }

    @PostMapping("/{id}/members")
    public R<Void> addMember(@PathVariable Long id,
                             @Valid @RequestBody AddMemberRequest req) {
        adminProjectService.addMember(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        adminProjectService.removeMember(id, userId);
        return R.ok();
    }

    /**
     * 成员变更历史（AD5.3）—— 谁在什么时候进了/出了这个项目。
     * 数据来自审计账本，不是单独的埋点表。
     */
    @GetMapping("/{id}/member-history")
    public R<List<AuditLogVO>> memberHistory(@PathVariable Long id) {
        return R.ok(adminAuditService.memberHistory(id));
    }
}
