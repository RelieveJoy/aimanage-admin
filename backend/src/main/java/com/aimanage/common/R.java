package com.aimanage.common;

import lombok.Data;

/**
 * 统一响应体：{@code {code, msg, data}}，见 docs/admin/API_SPEC.md §0.1。
 *
 * <p>HTTP 状态码与 {@code code} 保持一致，前端拦截器只需看一处。
 */
@Data
public class R<T> {

    public static final int OK = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int ERROR = 500;

    private int code;
    private String msg;
    private T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(OK, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(OK, "ok", data);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}
