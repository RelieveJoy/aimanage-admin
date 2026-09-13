package com.aimanage.request;

import com.aimanage.admin.AdminNotificationService;
import com.aimanage.common.BizException;
import com.aimanage.entity.JoinRequest;
import com.aimanage.entity.Notification;
import com.aimanage.entity.Project;
import com.aimanage.entity.ProjectMember;
import com.aimanage.entity.User;
import com.aimanage.mapper.JoinRequestMapper;
import com.aimanage.mapper.ProjectMapper;
import com.aimanage.mapper.ProjectMemberMapper;
import com.aimanage.mapper.UserMapper;
import com.aimanage.request.dto.SubmitJoinRequest;
import com.aimanage.security.UserContext;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 加人申请的<b>提交侧</b>（PM 端调用）。
 *
 * <p>PM 不能直接往项目里加人 —— 必须提交申请，由管理员批准后才写入
 * {@code project_member}。这样"项目成员"这件事完全归属管理员，
 * 与"明确每个项目的 PM 和成员是谁"这条需求一致。
 *
 * <p>本类代 PM 端实现。PM 前端只需加一个按钮调用 {@code POST /api/requests}。
 */
@Service
@RequiredArgsConstructor
public class RequestService {

    private final JoinRequestMapper joinRequestMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;
    private final AdminNotificationService notificationService;

    @Transactional
    public Long submit(SubmitJoinRequest req) {
        Long me = UserContext.currentUserId();

        Project project = projectMapper.selectById(req.getProjectId());
        if (project == null) {
            throw BizException.notFound("项目不存在");
        }
        if (project.getStatus() != null && project.getStatus() != Project.STATUS_ACTIVE) {
            throw BizException.badRequest("项目已归档，无法加人");
        }

        // 只有本项目的 PM 能提交申请
        if (!me.equals(project.getPmId())) {
            throw BizException.forbidden("只有本项目的项目经理可以提交加人申请");
        }

        User target = userMapper.selectById(req.getTargetUserId());
        if (target == null) {
            throw BizException.notFound("用户不存在");
        }
        if (target.getStatus() == null || target.getStatus() != 1) {
            throw BizException.badRequest("该账号已停用，无法加入项目");
        }

        if (findMember(project.getId(), target.getId()) != null) {
            throw BizException.conflict("该用户已在项目中");
        }

        // 避免同一个项目对同一个人反复提交
        Long dup = joinRequestMapper.selectCount(Wrappers.<JoinRequest>lambdaQuery()
                .eq(JoinRequest::getProjectId, project.getId())
                .eq(JoinRequest::getTargetUserId, target.getId())
                .eq(JoinRequest::getStatus, JoinRequest.STATUS_PENDING));
        if (dup != null && dup > 0) {
            throw BizException.conflict("已有一条待审批的申请，请等待管理员处理");
        }

        JoinRequest r = new JoinRequest();
        r.setProjectId(project.getId());
        r.setApplicantId(me);
        r.setTargetUserId(target.getId());
        r.setReason(StringUtils.hasText(req.getReason()) ? req.getReason() : null);
        r.setStatus(JoinRequest.STATUS_PENDING);
        joinRequestMapper.insert(r);

        // 通知所有管理员 —— 这就是 AD8 里"面向管理员的通知"的唯一来源
        User applicant = userMapper.selectById(me);
        notificationService.notifyAllAdmins(
                Notification.TYPE_JOIN_REQUEST,
                String.format("%s 申请把「%s」加入项目「%s」",
                        applicant == null ? "项目经理" : applicant.getName(),
                        target.getName(), project.getName()),
                "{\"requestId\":" + r.getId() + ",\"projectId\":" + project.getId() + "}");

        return r.getId();
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
    }
}
