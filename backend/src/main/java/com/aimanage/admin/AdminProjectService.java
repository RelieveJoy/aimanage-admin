package com.aimanage.admin;

import com.aimanage.admin.dto.AddMemberRequest;
import com.aimanage.admin.dto.CreateProjectRequest;
import com.aimanage.admin.dto.ProjectMemberVO;
import com.aimanage.admin.dto.ProjectVO;
import com.aimanage.admin.dto.UpdateProjectRequest;
import com.aimanage.audit.AuditContext;
import com.aimanage.audit.Auditable;
import com.aimanage.common.BizException;
import com.aimanage.common.PageResult;
import com.aimanage.entity.AuditLog;
import com.aimanage.entity.Department;
import com.aimanage.entity.Project;
import com.aimanage.entity.ProjectMember;
import com.aimanage.entity.User;
import com.aimanage.mapper.DepartmentMapper;
import com.aimanage.mapper.ProjectMapper;
import com.aimanage.mapper.ProjectMemberMapper;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.RoleEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目管理与项目成员（AD4 / AD5）。
 *
 * <p>成员挂项目下（{@code project_member}），一人可同时在多个项目。
 */
@Service
@RequiredArgsConstructor
public class AdminProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    // ---------------------------------------------------------------- 项目

    public PageResult<ProjectVO> page(String keyword, Integer status, long page, long size) {
        LambdaQueryWrapper<Project> w = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(Project::getName, keyword)
                    .or()
                    .like(Project::getCode, keyword));
        }
        if (status != null) {
            w.eq(Project::getStatus, status);
        }
        // 进行中的排前面，同状态内按创建时间倒序
        w.orderByDesc(Project::getStatus).orderByDesc(Project::getCreatedAt);

        Page<Project> result = projectMapper.selectPage(new Page<>(page, size), w);
        List<ProjectVO> records = result.getRecords().stream().map(ProjectVO::from).toList();
        fillPmAndCount(records);
        return new PageResult<>(result.getTotal(), page, size, records);
    }

    public ProjectVO detail(Long id) {
        ProjectVO vo = ProjectVO.from(requireProject(id));
        fillPmAndCount(List.of(vo));
        return vo;
    }

    /** 批量补 PM 姓名与成员数，避免列表里逐条查询 */
    private void fillPmAndCount(Collection<ProjectVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        List<Long> pmIds = vos.stream().map(ProjectVO::getPmId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> pmNames = pmIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(pmIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        List<Long> projectIds = vos.stream().map(ProjectVO::getId).toList();
        Map<Long, Long> counts = projectMemberMapper.selectList(
                        Wrappers.<ProjectMember>lambdaQuery().in(ProjectMember::getProjectId, projectIds))
                .stream()
                .collect(Collectors.groupingBy(ProjectMember::getProjectId, Collectors.counting()));

        vos.forEach(vo -> {
            vo.setPmName(pmNames.get(vo.getPmId()));
            vo.setMemberCount(counts.getOrDefault(vo.getId(), 0L).intValue());
        });
    }

    // ---------------------------------------------------------------- 新建

    @Auditable(type = AuditLog.TARGET_PROJECT, action = AuditLog.ACTION_CREATE)
    @Transactional
    public ProjectVO create(CreateProjectRequest req) {
        User pm = requireActivePm(req.getPmId());

        Project p = new Project();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPmId(pm.getId());
        p.setCode(nextProjectCode());
        p.setStatus(Project.STATUS_ACTIVE);
        projectMapper.insert(p);

        // 新增场景 ID 是插入后才有的，参数列表里拿不到，需显式告知切面
        AuditContext.targetId(p.getId());
        AuditContext.targetName(p.getName());
        AuditContext.change("项目", null, p.getName());
        AuditContext.change("项目经理", null, pm.getName());

        // PM 天然是项目成员，落一条 project_member，
        // 否则成员列表里会看不到 PM 自己
        insertMember(p.getId(), pm.getId(), ProjectMember.ROLE_PM);

        return detail(p.getId());
    }

    /**
     * 生成下一个项目编号：取已有的最大编号 +1，格式 PRJ-001。
     */
    private String nextProjectCode() {
        Project last = projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .likeRight(Project::getCode, "PRJ-")
                .orderByDesc(Project::getCode)
                .last("LIMIT 1"));

        int next = 1;
        if (last != null && StringUtils.hasText(last.getCode())) {
            try {
                next = Integer.parseInt(last.getCode().substring(4)) + 1;
            } catch (NumberFormatException ignored) {
                // 编号被人手工改坏了，退回按数量推算
                next = Math.toIntExact(projectMapper.selectCount(null)) + 1;
            }
        }
        return String.format("PRJ-%03d", next);
    }

    // ---------------------------------------------------------------- 修改

    @Auditable(type = AuditLog.TARGET_PROJECT, action = AuditLog.ACTION_UPDATE,
            projectIdArg = 0, targetIdArg = 0)
    @Transactional
    public ProjectVO update(Long id, UpdateProjectRequest req) {
        Project p = requireProject(id);
        AuditContext.targetName(p.getName());

        if (req.getName() != null && !req.getName().equals(p.getName())) {
            AuditContext.change("项目名称", p.getName(), req.getName());
            p.setName(req.getName());
        }
        if (req.getDescription() != null && !req.getDescription().equals(p.getDescription())) {
            AuditContext.change("项目描述", p.getDescription(), req.getDescription());
            p.setDescription(req.getDescription());
        }

        if (req.getPmId() != null && !req.getPmId().equals(p.getPmId())) {
            User newPm = requireActivePm(req.getPmId());
            User oldPm = userMapper.selectById(p.getPmId());
            AuditContext.change("项目经理",
                    oldPm == null ? null : oldPm.getName(), newPm.getName());

            // 原 PM 降为普通成员而不是踢出项目 —— 他可能仍在项目里干活
            ProjectMember oldPmMember = findMember(id, p.getPmId());
            if (oldPmMember != null) {
                oldPmMember.setRoleInProject(ProjectMember.ROLE_MEMBER);
                projectMemberMapper.updateById(oldPmMember);
            }

            ProjectMember target = findMember(id, newPm.getId());
            if (target == null) {
                insertMember(id, newPm.getId(), ProjectMember.ROLE_PM);
            } else {
                target.setRoleInProject(ProjectMember.ROLE_PM);
                projectMemberMapper.updateById(target);
            }

            p.setPmId(newPm.getId());
        }

        if (req.getStatus() != null && !req.getStatus().equals(p.getStatus())) {
            AuditContext.change("项目状态",
                    statusText(p.getStatus()), statusText(req.getStatus()));
            p.setStatus(req.getStatus());
        }

        projectMapper.updateById(p);
        return detail(id);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return null;
        }
        return status == Project.STATUS_ACTIVE ? "进行中" : "已归档";
    }

    // ---------------------------------------------------------------- 成员

    public List<ProjectMemberVO> members(Long projectId) {
        requireProject(projectId);

        List<ProjectMember> relations = projectMemberMapper.selectList(
                Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getJoinedAt));
        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = relations.stream().map(ProjectMember::getUserId).distinct().toList();
        Map<Long, User> userById = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<Long, String> deptNames = deptNameMap(userById.values());

        return relations.stream().map(r -> {
            User u = userById.get(r.getUserId());
            ProjectMemberVO vo = new ProjectMemberVO();
            vo.setUserId(r.getUserId());
            vo.setRoleInProject(r.getRoleInProject());
            vo.setJoinedAt(r.getJoinedAt());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setName(u.getName());
                vo.setSystemRole(u.getRole());
                vo.setStatus(u.getStatus());
                vo.setDeptName(deptNames.get(u.getDeptId()));
            }
            return vo;
        }).toList();
    }

    @Auditable(type = AuditLog.TARGET_PROJECT_MEMBER, action = AuditLog.ACTION_CREATE,
            projectIdArg = 0)
    @Transactional
    public void addMember(Long projectId, AddMemberRequest req) {
        Project project = requireProject(projectId);

        User u = userMapper.selectById(req.getUserId());
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            throw BizException.badRequest("该账号已停用，无法加入项目");
        }
        if (findMember(projectId, u.getId()) != null) {
            throw BizException.conflict("该用户已在项目中");
        }

        // 加进来的如果正是本项目的 PM，给 PM 角色
        String role = u.getId().equals(project.getPmId())
                ? ProjectMember.ROLE_PM
                : ProjectMember.ROLE_MEMBER;
        insertMember(projectId, u.getId(), role);

        // 这条记录同时是 AD5.3「成员变更历史」的数据来源
        AuditContext.targetId(u.getId());
        AuditContext.targetName(u.getName());
        AuditContext.change("项目成员", null, u.getName());
        AuditContext.remark("加入项目「" + project.getName() + "」");
    }

    @Auditable(type = AuditLog.TARGET_PROJECT_MEMBER, action = AuditLog.ACTION_DELETE,
            projectIdArg = 0, targetIdArg = 1)
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        Project project = requireProject(projectId);

        if (userId.equals(project.getPmId())) {
            throw BizException.conflict("不能把项目经理移出项目，请先更换项目经理");
        }
        ProjectMember member = findMember(projectId, userId);
        if (member == null) {
            throw BizException.notFound("该用户不在项目中");
        }

        User u = userMapper.selectById(userId);
        AuditContext.targetName(u == null ? null : u.getName());
        AuditContext.change("项目成员", u == null ? null : u.getName(), null);
        AuditContext.remark("移出项目「" + project.getName() + "」");

        projectMemberMapper.delete(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    // ---------------------------------------------------------------- 内部

    private void insertMember(Long projectId, Long userId, String roleInProject) {
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setUserId(userId);
        m.setRoleInProject(roleInProject);
        projectMemberMapper.insert(m);
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    /** PM 必须是启用的 PM 角色用户 —— 这是业务规则，不是格式校验 */
    private User requireActivePm(Long pmId) {
        User u = userMapper.selectById(pmId);
        if (u == null) {
            throw BizException.notFound("指定的项目经理不存在");
        }
        if (!RoleEnum.PM.name().equals(u.getRole())) {
            throw BizException.badRequest("项目经理必须是「项目经理」角色的用户");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            throw BizException.badRequest("该账号已停用，不能担任项目经理");
        }
        return u;
    }

    private Project requireProject(Long id) {
        Project p = projectMapper.selectById(id);
        if (p == null) {
            throw BizException.notFound("项目不存在");
        }
        return p;
    }

    private Map<Long, String> deptNameMap(Collection<User> users) {
        List<Long> deptIds = users.stream()
                .map(User::getDeptId).filter(Objects::nonNull).distinct().toList();
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }
}
