package com.aimanage.security;

import com.aimanage.common.BizException;

/**
 * 当前请求的登录用户，由 {@link AuthInterceptor} 写入、在 afterCompletion 中清理。
 *
 * <p>用 ThreadLocal 而非方法参数传递，是为了让 Service 层不必层层透传用户信息。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 可能为 null（如放行的公开接口） */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前用户，未登录直接抛 401 */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        return u;
    }

    public static Long currentUserId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
