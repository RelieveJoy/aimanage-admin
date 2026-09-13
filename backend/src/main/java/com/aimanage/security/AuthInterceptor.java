package com.aimanage.security;

import com.aimanage.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * ★ 鉴权拦截器 —— 本端全部安全性的落点。
 *
 * <p>处理顺序：
 * <ol>
 *   <li>放行跨域预检 OPTIONS</li>
 *   <li>解析 Authorization 头，无 token / 无效 / 过期 → 401</li>
 *   <li>写入 {@link UserContext}（ThreadLocal）</li>
 *   <li><b>路径以 /api/admin/ 开头且角色非 ADMIN → 403</b> ← 安全边界就在这一行</li>
 *   <li>存在 {@link RequireRole} 注解时，再做一次细粒度校验</li>
 * </ol>
 *
 * <p>前端路由守卫只是体验层；<b>越权测试（DoD 第 2 条）测的就是第 4 步</b>。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PREFIX = "/api/admin/";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 1. 跨域预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 解析 token
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            throw BizException.unauthorized("未登录或登录已过期");
        }
        LoginUser user = jwtUtil.parse(token);

        // 3. 绑定到当前线程
        UserContext.set(user);

        // 4. ★ 安全边界：管理端接口只允许 ADMIN
        if (request.getRequestURI().startsWith(ADMIN_PREFIX) && !user.isAdmin()) {
            throw BizException.forbidden("无权访问管理端接口");
        }

        // 5. 注解级细粒度校验（可选）
        if (handler instanceof HandlerMethod hm) {
            RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
            if (ann == null) {
                ann = hm.getBeanType().getAnnotation(RequireRole.class);
            }
            if (ann != null) {
                boolean allowed = Arrays.stream(ann.value())
                        .anyMatch(r -> r.name().equals(user.getRole()));
                if (!allowed) {
                    throw BizException.forbidden("当前角色无权访问该接口");
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程复用，必须清理，否则会串用户
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER)) {
            return header.substring(BEARER.length()).trim();
        }
        return null;
    }
}
