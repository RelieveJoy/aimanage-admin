package com.aimanage.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户的轻量快照，由 JWT 解析而来，不含密码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String name;

    /** 角色名，取值见 {@link RoleEnum} */
    private String role;

    /** 改密码时后端自增，用于让旧 token 失效 */
    private Integer tokenVersion;

    public boolean isAdmin() {
        return RoleEnum.ADMIN.name().equals(role);
    }
}
