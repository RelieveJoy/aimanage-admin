package com.aimanage.security;

import com.aimanage.common.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。三端共用同一份实现。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${aimanage.jwt.secret}") String secret,
                   @Value("${aimanage.jwt.expire}") long expireMillis) {
        // HS256 要求密钥至少 256 bit（32 字节）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireMillis;
    }

    /**
     * 签发 token。
     *
     * @param tokenVersion 取自 user.token_version，改密码时后端自增，
     *                     使已签发的旧 token 在下次请求时失效
     */
    public String generate(Long userId, String username, String name,
                           String role, Integer tokenVersion) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("name", name)
                .claim("role", role)
                .claim("tv", tokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并验签。任何问题（签名错、过期、格式错）都统一抛 401，
     * 不向外泄露具体原因。
     */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            LoginUser u = new LoginUser();
            u.setUserId(Long.valueOf(c.getSubject()));
            u.setUsername(c.get("username", String.class));
            u.setName(c.get("name", String.class));
            u.setRole(c.get("role", String.class));
            u.setTokenVersion(c.get("tv", Integer.class));
            return u;
        } catch (JwtException | IllegalArgumentException e) {
            throw BizException.unauthorized("登录已过期，请重新登录");
        }
    }
}
