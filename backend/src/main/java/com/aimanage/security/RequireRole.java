package com.aimanage.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明接口所需角色，由 {@link AuthInterceptor} 统一校验（方案 A1.2 "接口级写校验"）。
 *
 * <p>注意：{@code /api/admin/**} 路径的角色校验已由拦截器硬编码兜底，
 * 本注解用于**额外的、更细粒度**的限制，例如某个接口只允许 PM 调用。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，满足其一即放行 */
    RoleEnum[] value();
}
