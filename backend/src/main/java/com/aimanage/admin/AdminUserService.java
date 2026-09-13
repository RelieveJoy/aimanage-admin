package com.aimanage.admin;

import com.aimanage.admin.dto.CreateUserRequest;
import com.aimanage.admin.dto.UpdateUserRequest;
import com.aimanage.audit.AuditContext;
import com.aimanage.audit.Auditable;
import com.aimanage.auth.dto.UserVO;
import com.aimanage.common.BizException;
import com.aimanage.common.PageResult;
import com.aimanage.entity.AuditLog;
import com.aimanage.entity.Department;
import com.aimanage.entity.User;
import com.aimanage.mapper.DepartmentMapper;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DepartmentMapper departmentMapper;
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
        fillDeptName(records);
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    public UserVO detail(Long id) {
        UserVO vo = UserVO.from(requireUser(id));
        fillDeptName(List.of(vo));
        return vo;
    }

    /**
     * 批量回填部门名称。
     * 一次查出涉及到的部门做映射，避免在循环里逐条 selectById。
     */
    private void fillDeptName(Collection<UserVO> vos) {
        List<Long> deptIds = vos.stream()
                .map(UserVO::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (deptIds.isEmpty()) {
            return;
        }

        Map<Long, String> nameById = departmentMapper
                .selectBatchIds(deptIds)
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName,
                        (a, b) -> a));

        vos.forEach(vo -> vo.setDeptName(nameById.get(vo.getDeptId())));
    }

    // ---------------------------------------------------------------- 新增

    @Auditable(type = AuditLog.TARGET_USER, action = AuditLog.ACTION_CREATE)
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
        Long deptId = normalizeDeptId(req.getDeptId());

        User u = new User();
        u.setUsername(req.getUsername());
        u.setName(req.getName());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRole(req.getRole());
        u.setDeptId(deptId);
        u.setStatus(1);
        u.setTokenVersion(0);
        userMapper.insert(u);

        AuditContext.targetId(u.getId());
        AuditContext.targetName(u.getName());
        AuditContext.change("账号", null, u.getName());
        AuditContext.change("角色", null, roleText(u.getRole()));

        return UserVO.from(u);
    }

    private String roleText(String role) {
        if (RoleEnum.PM.name().equals(role)) {
            return "项目经理";
        }
        if (RoleEnum.MEMBER.name().equals(role)) {
            return "团队成员";
        }
        return role;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "启用" : "停用";
    }

    // ---------------------------------------------------------------- 修改

    @Auditable(type = AuditLog.TARGET_USER, action = AuditLog.ACTION_UPDATE, targetIdArg = 0)
    @Transactional
    public UserVO update(Long id, UpdateUserRequest req) {
        User u = requireUser(id);
        AuditContext.targetName(u.getName());

        if (req.getName() != null && !req.getName().equals(u.getName())) {
            AuditContext.change("姓名", u.getName(), req.getName());
            u.setName(req.getName());
        }

        if (req.getRole() != null && !req.getRole().equals(u.getRole())) {
            if (!ASSIGNABLE_ROLES.contains(req.getRole())) {
                throw BizException.badRequest("角色只能是 PM 或 MEMBER");
            }
            AuditContext.change("角色", roleText(u.getRole()), roleText(req.getRole()));
            u.setRole(req.getRole());
        }

        if (req.getDeptId() != null) {
            String before = deptNameOf(u.getDeptId());
            if (req.getDeptId() == 0L) {
                // 约定 deptId = 0 表示"不分配"。
                // 不能只 setDeptId(null)：MyBatis-Plus 的 updateById 默认忽略 null 字段，
                // 置空必须走 UpdateWrapper 显式 set。
                userMapper.update(null, Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, u.getId())
                        .set(User::getDeptId, null));
                u.setDeptId(null);
                AuditContext.change("部门", before, null);
            } else {
                requireDeptExists(req.getDeptId());
                if (!req.getDeptId().equals(u.getDeptId())) {
                    AuditContext.change("部门", before, deptNameOf(req.getDeptId()));
                }
                u.setDeptId(req.getDeptId());
            }
        }

        if (req.getStatus() != null && !req.getStatus().equals(u.getStatus())) {
            if (req.getStatus() == 0) {
                guardDisable(u);
            }
            AuditContext.change("账号状态", statusText(u.getStatus()), statusText(req.getStatus()));
            u.setStatus(req.getStatus());
        }

        userMapper.updateById(u);
        return UserVO.from(u);
    }

    private String deptNameOf(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Department d = departmentMapper.selectById(deptId);
        return d == null ? null : d.getName();
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
    @Auditable(type = AuditLog.TARGET_USER, action = AuditLog.ACTION_UPDATE, targetIdArg = 0)
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User u = requireUser(id);
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setTokenVersion(u.getTokenVersion() == null ? 1 : u.getTokenVersion() + 1);
        userMapper.updateById(u);

        // 审计里绝不能记密码本身，只记"发生过重置"这件事
        AuditContext.targetName(u.getName());
        AuditContext.change("密码", null, "已被重置");
        AuditContext.remark("管理员重置密码，该用户登录状态已失效");
    }

    // ---------------------------------------------------------------- 内部

    private User requireUser(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        return u;
    }

    /**
     * 归一化 deptId：0 与 null 都表示"不分配"，其余值必须真实存在。
     */
    private Long normalizeDeptId(Long deptId) {
        if (deptId == null || deptId == 0L) {
            return null;
        }
        requireDeptExists(deptId);
        return deptId;
    }

    /** deptId 允许为 null（未分配部门），但填了就必须真实存在 */
    private void requireDeptExists(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (departmentMapper.selectById(deptId) == null) {
            throw BizException.badRequest("所选部门不存在");
        }
    }
}
