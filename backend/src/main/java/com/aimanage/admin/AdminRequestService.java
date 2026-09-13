package com.aimanage.admin;

import com.aimanage.admin.dto.JoinRequestVO;
import com.aimanage.admin.dto.ReviewRequest;
import com.aimanage.audit.AuditContext;
import com.aimanage.audit.Auditable;
import com.aimanage.common.BizException;
import com.aimanage.common.PageResult;
import com.aimanage.entity.AuditLog;
import com.aimanage.entity.Department;
import com.aimanage.entity.JoinRequest;
import com.aimanage.entity.Notification;
import com.aimanage.entity.Project;
import com.aimanage.entity.ProjectMember;
import com.aimanage.entity.User;
import com.aimanage.mapper.DepartmentMapper;
import com.aimanage.mapper.JoinRequestMapper;
import com.aimanage.mapper.ProjectMapper;
import com.aimanage.mapper.ProjectMemberMapper;
import com.aimanage.mapper.UserMapper;
import com.aimanage.security.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 加人申请的审批（AD8 管理侧）。
 *
 * <p>本期只有这一种申请类型 —— 方案 B 的取舍：先把一条完整链路打通，
 * 而不是铺开做多事件源矩阵。
 */
@Service
@RequiredArgsConstructor
public class AdminRequestService {

    private final JoinRequestMapper joinRequestMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final AdminNotificationService notificationService;

    // ---------------------------------------------------------------- 查询

    public PageResult<JoinRequestVO> page(String status, long page, long size) {
        LambdaQueryWrapper<JoinRequest> w = Wrappers.lambdaQuery();
        if (StringUtils.hasText(status)) {
            w.eq(JoinRequest::getStatus, status);
        }
        // 待审批的排最前，其余按时间倒序
        w.orderByAsc(JoinRequest::getStatus).orderByDesc(JoinRequest::getCreatedAt);

        Page<JoinRequest> result = joinRequestMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(result.getTotal(), page, size, toVOList(result.getRecords()));
    }

    public long pendingCount() {
        Long c = joinRequestMapper.selectCount(Wrappers.<JoinRequest>lambdaQuery()
                .eq(JoinRequest::getStatus, JoinRequest.STATUS_PENDING));
        return c == null ? 0 : c;
    }

    // ---------------------------------------------------------------- 批准

    @Auditable(type = AuditLog.TARGET_PROJECT_MEMBER, action = AuditLog.ACTION_CREATE)
    @Transactional
    public void approve(Long requestId, ReviewRequest req) {
        JoinRequest r = requirePending(requestId);
        Project project = projectMapper.selectById(r.getProjectId());
        if (project == null) {
            throw BizException.conflict("项目已不存在，无法批准");
        }

        User target = userMapper.selectById(r.getTargetUserId());
        if (target == null) {
            throw BizException.conflict("申请人已不存在，无法批准");
        }
        if (target.getStatus() == null || target.getStatus() != 1) {
            throw BizException.conflict("该用户账号已停用，无法加入项目");
        }
        if (findMember(r.getProjectId(), r.getTargetUserId()) != null) {
            // 期间可能已通过其它途径加入，此时批准无意义
            throw BizException.conflict("该用户已在项目中，无需批准");
        }

        // 加进来的如果正是本项目的 PM，给 PM 角色
        String role = r.getTargetUserId().equals(project.getPmId())
                ? ProjectMember.ROLE_PM
                : ProjectMember.ROLE_MEMBER;

        ProjectMember m = new ProjectMember();
        m.setProjectId(r.getProjectId());
        m.setUserId(r.getTargetUserId());
        m.setRoleInProject(role);
        projectMemberMapper.insert(m);

        // 项目 ID 来自申请记录而非入参，必须显式告诉切面
        AuditContext.projectId(r.getProjectId());
        AuditContext.targetId(r.getTargetUserId());
        AuditContext.targetName(target.getName());
        AuditContext.change("项目成员", null, target.getName());
        AuditContext.remark("批准加人申请，加入项目「" + project.getName() + "」");

        markReviewed(r, JoinRequest.STATUS_APPROVED, req.getComment());

        notificationService.send(r.getApplicantId(), Notification.TYPE_REQUEST_APPROVED,
                String.format("你为项目「%s」申请加入的「%s」已获批准",
                        project.getName(), target.getName()),
                "{\"projectId\":" + project.getId() + "}");
    }

    // ---------------------------------------------------------------- 拒绝

    @Auditable(type = AuditLog.TARGET_JOIN_REQUEST, action = AuditLog.ACTION_UPDATE)
    @Transactional
    public void reject(Long requestId, ReviewRequest req) {
        JoinRequest r = requirePending(requestId);

        // 拒绝必须给理由 —— 否则申请人不知道为什么被拒
        if (!StringUtils.hasText(req.getComment())) {
            throw BizException.badRequest("拒绝时必须填写理由");
        }

        Project project = projectMapper.selectById(r.getProjectId());
        User target = userMapper.selectById(r.getTargetUserId());
        String projectName = project == null ? "（已删除的项目）" : project.getName();
        String targetName = target == null ? "（已删除的用户）" : target.getName();

        AuditContext.projectId(r.getProjectId());
        AuditContext.targetId(r.getId());
        AuditContext.targetName(targetName);
        AuditContext.change("申请状态", "待审批", "已拒绝");
        AuditContext.remark("拒绝理由：" + req.getComment());

        markReviewed(r, JoinRequest.STATUS_REJECTED, req.getComment());

        notificationService.send(r.getApplicantId(), Notification.TYPE_REQUEST_REJECTED,
                String.format("你为项目「%s」申请加入的「%s」被拒绝：%s",
                        projectName, targetName, req.getComment()),
                "{\"projectId\":" + r.getProjectId() + "}");
    }

    // ---------------------------------------------------------------- 内部

    private void markReviewed(JoinRequest r, String status, String comment) {
        r.setStatus(status);
        r.setReviewerId(UserContext.currentUserId());
        r.setReviewComment(comment);
        r.setReviewedAt(LocalDateTime.now());
        joinRequestMapper.updateById(r);
    }

    private JoinRequest requirePending(Long id) {
        JoinRequest r = joinRequestMapper.selectById(id);
        if (r == null) {
            throw BizException.notFound("申请不存在");
        }
        if (!JoinRequest.STATUS_PENDING.equals(r.getStatus())) {
            throw BizException.conflict("该申请已处理过了");
        }
        return r;
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }

    /** 批量补齐申请人/项目/目标用户的名称，避免逐条查 */
    private List<JoinRequestVO> toVOList(List<JoinRequest> list) {
        if (list.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = list.stream()
                .flatMap(r -> Stream.of(r.getApplicantId(), r.getTargetUserId(), r.getReviewerId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<Long> projectIds = list.stream().map(JoinRequest::getProjectId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> projectNames = projectIds.isEmpty() ? Map.of()
                : projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Project::getName, (a, b) -> a));

        Map<Long, String> deptNames = deptNames(users.values());

        return list.stream().map(r -> {
            JoinRequestVO vo = new JoinRequestVO();
            vo.setId(r.getId());
            vo.setProjectId(r.getProjectId());
            vo.setProjectName(projectNames.get(r.getProjectId()));
            vo.setApplicantId(r.getApplicantId());
            vo.setReason(r.getReason());
            vo.setStatus(r.getStatus());
            vo.setReviewerId(r.getReviewerId());
            vo.setReviewComment(r.getReviewComment());
            vo.setReviewedAt(r.getReviewedAt());
            vo.setCreatedAt(r.getCreatedAt());

            User applicant = users.get(r.getApplicantId());
            if (applicant != null) {
                vo.setApplicantName(applicant.getName());
            }
            User target = users.get(r.getTargetUserId());
            if (target != null) {
                vo.setTargetUserId(r.getTargetUserId());
                vo.setTargetName(target.getName());
                vo.setTargetUsername(target.getUsername());
                vo.setTargetDeptName(deptNames.get(target.getDeptId()));
            }
            User reviewer = users.get(r.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getName());
            }

            // 审批前先让管理员看到"这人已经在项目里了"，避免批了才发现重复
            vo.setTargetAlreadyInProject(findMember(r.getProjectId(), r.getTargetUserId()) != null);
            return vo;
        }).toList();
    }

    private Map<Long, String> deptNames(Collection<User> users) {
        List<Long> deptIds = users.stream()
                .map(User::getDeptId).filter(Objects::nonNull).distinct().toList();
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }
}
