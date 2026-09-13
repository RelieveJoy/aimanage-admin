package com.aimanage.common;

import lombok.Getter;

/**
 * 业务异常。抛出后由 {@link GlobalExceptionHandler} 转成统一响应体。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /** 400 参数校验失败 */
    public static BizException badRequest(String msg) {
        return new BizException(R.BAD_REQUEST, msg);
    }

    /** 401 未登录 / 凭证无效 */
    public static BizException unauthorized(String msg) {
        return new BizException(R.UNAUTHORIZED, msg);
    }

    /** 403 已登录但无权限 */
    public static BizException forbidden(String msg) {
        return new BizException(R.FORBIDDEN, msg);
    }

    /** 404 资源不存在 */
    public static BizException notFound(String msg) {
        return new BizException(R.NOT_FOUND, msg);
    }

    /** 409 业务冲突，如用户名重复、部门非空不可删 */
    public static BizException conflict(String msg) {
        return new BizException(R.CONFLICT, msg);
    }
}
