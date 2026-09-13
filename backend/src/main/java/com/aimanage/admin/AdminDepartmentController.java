package com.aimanage.admin;

import com.aimanage.admin.dto.CreateDepartmentRequest;
import com.aimanage.admin.dto.DepartmentVO;
import com.aimanage.admin.dto.UpdateDepartmentRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 · 组织架构（AD3）。
 *
 * <p>路径以 {@code /api/admin/} 开头，自动受 {@code AuthInterceptor} 的 ADMIN 保护。
 */
@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping
    public R<List<DepartmentVO>> tree() {
        return R.ok(adminDepartmentService.tree());
    }

    @PostMapping
    public R<DepartmentVO> create(@Valid @RequestBody CreateDepartmentRequest req) {
        return R.ok(adminDepartmentService.create(req));
    }

    @PatchMapping("/{id}")
    public R<DepartmentVO> update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateDepartmentRequest req) {
        return R.ok(adminDepartmentService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminDepartmentService.delete(id);
        return R.ok();
    }
}
