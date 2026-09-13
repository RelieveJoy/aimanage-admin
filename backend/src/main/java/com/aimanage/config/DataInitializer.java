package com.aimanage.config;

import com.aimanage.entity.User;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.RoleEnum;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 种子数据：首次启动时创建一个管理员账号。
 *
 * <p>密码在运行时用 BCrypt 编码，因此 schema.sql 里不存哈希，
 * 也就不存在"硬编码哈希"的问题。
 *
 * <p>幂等：系统中已存在任一启用的 ADMIN 时不做任何事。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${aimanage.seed.admin-username}")
    private String adminUsername;

    @Value("${aimanage.seed.admin-password}")
    private String adminPassword;

    @Value("${aimanage.seed.admin-name}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, RoleEnum.ADMIN.name()));

        if (count != null && count > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setName(adminName);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setStatus(1);
        admin.setTokenVersion(0);
        userMapper.insert(admin);

        log.info("""

                ========================================================
                 已创建种子管理员账号
                   用户名 : {}
                   密码   : {}   ← 首次登录后请立即修改
                ========================================================
                """, adminUsername, adminPassword);
    }
}
