package com.aimanage.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户（AD2.2 新建 PM / AD2.3 新建 Member）。
 *
 * <p>{@code role} 只接受 PM / MEMBER —— 校验在 Service 层做，
 * 因为"不能授予 ADMIN"是业务规则而非格式规则。
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "请输入用户名")
    @Size(max = 50, message = "用户名过长")
    private String username;

    @NotBlank(message = "请输入姓名")
    @Size(max = 50, message = "姓名过长")
    private String name;

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String password;

    /** PM / MEMBER */
    @NotBlank(message = "请选择角色")
    private String role;

    private Long deptId;
}
