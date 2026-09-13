package com.aimanage.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把异常统一收敛成 {@link R}，并让 HTTP 状态码与 code 对齐。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 —— 预期内的错误，用 warn 级别，不打堆栈 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getCode()).body(R.fail(e.getCode(), e.getMessage()));
    }

    /** @Valid 校验失败 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<R<Void>> handleValidation(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError fe = ex.getBindingResult().getFieldError();
            msg = fe == null ? msg : fe.getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError fe = ex.getBindingResult().getFieldError();
            msg = fe == null ? msg : fe.getDefaultMessage();
        }
        log.warn("参数校验失败: {}", msg);
        return ResponseEntity.status(R.BAD_REQUEST).body(R.fail(R.BAD_REQUEST, msg));
    }

    /**
     * 路由不存在。
     *
     * <p>必须单独处理：Spring Boot 3.2 把未匹配的请求抛成 NoResourceFoundException，
     * 若不拦截就会被下面的兜底处理器转成 500 —— 把"接口不存在"变成
     * "服务器内部错误"，排查时非常误导。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoRoute(NoResourceFoundException e) {
        return ResponseEntity.status(R.NOT_FOUND)
                .body(R.fail(R.NOT_FOUND, "接口不存在：" + e.getResourcePath()));
    }

    /** 请求方法不对（用 GET 调了 POST 接口之类） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405)
                .body(R.fail(405, "请求方法不支持：" + e.getMethod()));
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        log.error("未预期异常", e);
        return ResponseEntity.status(R.ERROR).body(R.fail(R.ERROR, "服务器内部错误"));
    }
}
