package com.aimanage.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户（AD2.4 / AD2.5）。
 *
 * <p>字段为 {@code null} 表示不修改 —— 这是 PATCH 语义，
 * 因此不能用 {@code @NotNull} 之类的必填校验。
 */
@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "姓名过长")
    private String name;

    /** PM / MEMBER；改为 ADMIN 会被拒绝 */
    private String role;

    /**
     * 部门 ID。<b>传 0 表示"不分配"</b>（因为 PATCH 语义下 null 是"不修改"，
     * 需要一个哨兵值来表达"清空"）。
     */
    private Long deptId;

    /** 1 启用 / 0 停用 */
    private Integer status;
}
