package com.aimanage.admin;

import com.aimanage.admin.dto.CreateUserRequest;
import com.aimanage.admin.dto.UpdateUserRequest;
import com.aimanage.auth.dto.UserVO;
import com.aimanage.common.BizException;
import com.aimanage.common.PageResult;
import com.aimanage.entity.User;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.RoleEnum;
import com.aimanage.security.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 用户管理业务逻辑（AD2）。
 *
 * <p>本类的校验都是<b>规则</b>而非格式：角色只能授 PM/MEMBER、不能停用自己、
 * 不能停用最后一个管理员。这些规则代码内置，不做成配置项（方案 Table 5 Won't）。
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    /** 管理员可通过界面授予的角色。Admin 自身不在其中。 */
    private static final Set<String> ASSIGNABLE_ROLES =
            Set.of(RoleEnum.PM.name(), RoleEnum.MEMBER.name());

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------- 查询

    public PageResult<UserVO> page(String keyword, String role, Long deptId,
                                   Integer status, long page, long size) {
        LambdaQueryWrapper<User> w = Wrappers.lambdaQuery();

        if (StringUtils.hasText(keyword)) {
            // 用户名或姓名模糊匹配，用 and(...) 包住，避免 or 逃逸影响其他条件
            w.and(x -> x.like(User::getUsername, keyword)
                    .or()
                    .like(User::getName, keyword));
        }
        if (StringUtils.hasText(role)) {
            w.eq(User::getRole, role);
        }
        if (deptId != null) {
            w.eq(User::getDeptId, deptId);
        }
        if (status != null) {
            w.eq(User::getStatus, status);
        }
        w.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), w);
        List<UserVO> records = result.getRecords().stream().map(UserVO::from).toList();
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    public UserVO detail(Long id) {
        return UserVO.from(requireUser(id));
    }

    // ---------------------------------------------------------------- 新增

    @Transactional
    public UserVO create(CreateUserRequest req) {
        if (!ASSIGNABLE_ROLES.contains(req.getRole())) {
            throw BizException.badRequest("角色只能是 PM 或 MEMBER");
        }
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw BizException.conflict("用户名已存在");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setName(req.getName());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(req.getRole());
        u.setDeptId(req.getDeptId());
        u.setStatus(1);
        u.setTokenVersion(0);
        userMapper.insert(u);

        return UserVO.from(u);
    }

    // ---------------------------------------------------------------- 修改

    @Transactional
    public UserVO update(Long id, UpdateUserRequest req) {
        User u = requireUser(id);

        if (req.getName() != null) {
            u.setName(req.getName());
        }

        if (req.getRole() != null) {
            if (!ASSIGNABLE_ROLES.contains(req.getRole())) {
                throw BizException.badRequest("角色只能是 PM 或 MEMBER");
            }
            u.setRole(req.getRole());
        }

        if (req.getDeptId() != null) {
            u.setDeptId(req.getDeptId());
        }

        if (req.getStatus() != null) {
            if (req.getStatus() == 0) {
                guardDisable(u);
            }
            u.setStatus(req.getStatus());
        }

        userMapper.updateById(u);
        return UserVO.from(u);
    }

    /**
     * 停用前的两道闸：不能停自己、不能把系统里最后一个启用的管理员停掉。
     */
    private void guardDisable(User target) {
        if (target.getId().equals(UserContext.currentUserId())) {
            throw BizException.conflict("不能停用当前登录账号");
        }
        if (RoleEnum.ADMIN.name().equals(target.getRole())) {
            Long activeAdmins = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getRole, RoleEnum.ADMIN.name())
                    .eq(User::getStatus, 1));
            if (activeAdmins != null && activeAdmins <= 1) {
                throw BizException.conflict("系统必须保留至少一个启用的管理员");
            }
        }
    }

    // ---------------------------------------------------------------- 重置密码

    /**
     * 重置密码，同时自增 {@code token_version}，
     * 使该用户已签发的 token 在下次请求时失效。
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User u = requireUser(id);
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setTokenVersion(u.getTokenVersion() == null ? 1 : u.getTokenVersion() + 1);
        userMapper.updateById(u);
    }

    // ---------------------------------------------------------------- 内部

    private User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        return u;
    }
}
