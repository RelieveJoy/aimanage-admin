package com.aimanage.config;

import com.aimanage.entity.Department;
import com.aimanage.entity.User;
import com.aimanage.mapper.DepartmentMapper;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.RoleEnum;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 种子数据：公司根节点 + 管理员账号。
 *
 * <p>密码在运行时用 BCrypt 编码，因此 schema.sql 里不存哈希，
 * 也就不存在"硬编码哈希"的问题。
 *
 * <p>幂等：每项各自检查是否已存在，重复启动不会产生脏数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${aimanage.seed.company-name}")
    private String companyName;

    @Value("${aimanage.seed.admin-username}")
    private String adminUsername;

    @Value("${aimanage.seed.admin-password}")
    private String adminPassword;

    @Value("${aimanage.seed.admin-name}")
    private String adminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCompany();
        seedAdmin();
    }

    /**
     * 建公司根节点。组织架构树是两层：公司 → 部门，
     * 根节点是部门树的锚点，必须先于任何部门存在。
     */
    private void seedCompany() {
        Long existing = departmentMapper.selectCount(
                Wrappers.<Department>lambdaQuery().eq(Department::getParentId, 0L));
        if (existing != null && existing > 0) {
            return;
        }

        Department root = new Department();
        root.setName(companyName);
        root.setParentId(0L);
        root.setSort(0);
        departmentMapper.insert(root);

        log.info("已创建公司根节点：{}（id={}）", companyName, root.getId());
    }

    private void seedAdmin() {
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
