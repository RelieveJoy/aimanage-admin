package com.aimanage.admin;

import com.aimanage.admin.dto.CreateUserRequest;
import com.aimanage.admin.dto.ResetPasswordRequest;
import com.aimanage.admin.dto.UpdateUserRequest;
import com.aimanage.auth.dto.UserVO;
import com.aimanage.common.PageResult;
import com.aimanage.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 · 用户管理（AD2）。
 *
 * <p>路径以 {@code /api/admin/} 开头，由 {@code AuthInterceptor} 强制
 * {@code role == ADMIN} —— 本类不需要任何额外的权限注解。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public R<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(adminUserService.page(keyword, role, deptId, status, page, size));
    }

    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        return R.ok(adminUserService.detail(id));
    }

    @PostMapping
    public R<UserVO> create(@Valid @RequestBody CreateUserRequest req) {
        return R.ok(adminUserService.create(req));
    }

    @PatchMapping("/{id}")
    public R<UserVO> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return R.ok(adminUserService.update(id, req));
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id,
                                 @Valid @RequestBody ResetPasswordRequest req) {
        adminUserService.resetPassword(id, req.getNewPassword());
        return R.ok();
    }
}
