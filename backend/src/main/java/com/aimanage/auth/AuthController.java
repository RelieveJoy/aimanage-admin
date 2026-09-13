package com.aimanage.auth;

import com.aimanage.auth.dto.LoginRequest;
import com.aimanage.auth.dto.LoginResponse;
import com.aimanage.auth.dto.UserVO;
import com.aimanage.common.BizException;
import com.aimanage.common.R;
import com.aimanage.entity.User;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.JwtUtil;
import com.aimanage.security.LoginUser;
import com.aimanage.security.UserContext;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口 —— 三端共用，Admin 端只做 UI 独立，接口完全复用。
 *
 * <p>{@code /api/auth/login} 在 {@link com.aimanage.config.WebConfig} 中被排除在
 * 拦截器之外，其余接口均需登录。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        User u = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));

        // 账号不存在与密码错误返回同一提示，不泄露账号是否注册
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            throw BizException.forbidden("账号已停用，请联系管理员");
        }

        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getName(),
                u.getRole(), u.getTokenVersion());
        return R.ok(new LoginResponse(token, UserVO.from(u)));
    }

    /**
     * 登出。JWT 无状态，服务端无需做事，前端清除本地 token 即可。
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    /**
     * 当前登录用户，用于前端刷新页面后恢复状态。
     */
    @GetMapping("/me")
    public R<UserVO> me() {
        LoginUser lu = UserContext.require();
        User u = userMapper.selectById(lu.getUserId());
        if (u == null) {
            throw BizException.unauthorized("登录已过期，请重新登录");
        }
        return R.ok(UserVO.from(u));
    }
}
