package com.aimanage.config;

import com.aimanage.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：注册鉴权拦截器、跨域、密码编码器。
 *
 * <p>拦截器注册在 {@code /api/**} 上；新增的 {@code /api/admin/**} 接口
 * <b>自动获得角色保护</b>，无需逐个加注解。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 登录接口本身当然不能要求先登录
                .excludePathPatterns(
                        "/api/auth/login",
                        "/error"
                );
    }

    /**
     * 开发期跨域。
     * Admin 端独立前端跑在 Vite 上（默认 5173），与后端 8080 不同源。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 只引 spring-security-crypto，用它做 BCrypt 哈希；
     * 不用 Security 的过滤器链（见 ARCHITECTURE.md §1.1）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
