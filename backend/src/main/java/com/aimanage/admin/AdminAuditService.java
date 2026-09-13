package com.aimanage.admin;

import com.aimanage.admin.dto.AuditLogVO;
import com.aimanage.common.BizException;
import com.aimanage.common.PageResult;
import com.aimanage.entity.AuditLog;
import com.aimanage.entity.Project;
import com.aimanage.entity.User;
import com.aimanage.mapper.AuditLogMapper;
import com.aimanage.mapper.ProjectMapper;
import com.aimanage.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审计账本的<b>读取</b>侧。
 *
 * <p>写入侧在 {@link com.aimanage.audit.AuditAspect}，本类只读不写。
 * 同时服务 AD5.3（成员变更历史）与 AD6（全局检索）。
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    /**
     * 某个项目的成员变更历史（AD5.3）。
     *
     * <p>不需要单独的埋点表 —— 成员增删本就会被审计切面记录，
     * 这里只是把它筛出来。
     */
    public List<AuditLogVO> memberHistory(Long projectId) {
        List<AuditLog> logs = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(AuditLog::getProjectId, projectId)
                .eq(AuditLog::getTargetType, AuditLog.TARGET_PROJECT_MEMBER)
                .orderByDesc(AuditLog::getCreatedAt));
        return toVOList(logs);
    }

    /**
     * 全局审计检索（AD6）—— 责任判定的主要入口。
     *
     * <p>时间是按"天"筛的直觉来处理的：{@code endTime} 传 {@code 2026-08-12} 时
     * 会自动扩到当天 23:59:59，否则用户会发现"今天发生的变更筛不出来"。
     */
    public PageResult<AuditLogVO> search(Long projectId, Long operatorId, String targetType,
                                         String field, String startTime, String endTime,
                                         String keyword, long page, long size) {
        LambdaQueryWrapper<AuditLog> w = Wrappers.lambdaQuery();

        if (projectId != null) {
            w.eq(AuditLog::getProjectId, projectId);
        }
        if (operatorId != null) {
            w.eq(AuditLog::getOperatorId, operatorId);
        }
        if (StringUtils.hasText(targetType)) {
            w.eq(AuditLog::getTargetType, targetType);
        }
        if (StringUtils.hasText(field)) {
            w.eq(AuditLog::getField, field);
        }
        if (StringUtils.hasText(startTime)) {
            w.ge(AuditLog::getCreatedAt, parseTime(startTime, false));
        }
        if (StringUtils.hasText(endTime)) {
            w.le(AuditLog::getCreatedAt, parseTime(endTime, true));
        }
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(AuditLog::getTargetName, keyword)
                    .or().like(AuditLog::getRemark, keyword)
                    .or().like(AuditLog::getBeforeValue, keyword)
                    .or().like(AuditLog::getAfterValue, keyword));
        }
        w.orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> result = auditLogMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(result.getTotal(), page, size, toVOList(result.getRecords()));
    }

    /** 单条详情，含完整前后值与备注 */
    public AuditLogVO detail(Long id) {
        AuditLog entry = auditLogMapper.selectById(id);
        if (entry == null) {
            throw BizException.notFound("审计记录不存在");
        }
        return toVOList(List.of(entry)).get(0);
    }

    /**
     * 宽松解析时间：既接受 {@code 2026-08-12}，也接受 {@code 2026-08-12 14:32:10}。
     * {@code toEndOfDay} 为 true 且只给了日期时，补到当天最后一刻。
     */
    private LocalDateTime parseTime(String raw, boolean toEndOfDay) {
        String s = raw.trim();
        try {
            if (s.length() <= 10) {
                LocalDate d = LocalDate.parse(s);
                return toEndOfDay ? d.atTime(23, 59, 59) : d.atStartOfDay();
            }
            return LocalDateTime.parse(s.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw BizException.badRequest("时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 把审计记录补上操作人姓名与项目名称。
     * 批量查两张表做映射，避免逐条 selectById。
     */
    List<AuditLogVO> toVOList(List<AuditLog> logs) {
        if (logs.isEmpty()) {
            return List.of();
        }

        Map<Long, String> operatorNames = nameMap(
                selectUsers(distinct(logs, AuditLog::getOperatorId)),
                User::getId, User::getName);
        Map<Long, String> projectNames = nameMap(
                selectProjects(distinct(logs, AuditLog::getProjectId)),
                Project::getId, Project::getName);

        return logs.stream().map(l -> {
            AuditLogVO vo = new AuditLogVO();
            vo.setId(l.getId());
            vo.setOperatorId(l.getOperatorId());
            vo.setOperatorName(operatorNames.get(l.getOperatorId()));
            vo.setOperatorRole(l.getOperatorRole());
            vo.setProjectId(l.getProjectId());
            vo.setProjectName(projectNames.get(l.getProjectId()));
            vo.setTargetType(l.getTargetType());
            vo.setTargetId(l.getTargetId());
            vo.setTargetName(l.getTargetName());
            vo.setField(l.getField());
            vo.setBeforeValue(l.getBeforeValue());
            vo.setAfterValue(l.getAfterValue());
            vo.setAction(l.getAction());
            vo.setRemark(l.getRemark());
            vo.setCreatedAt(l.getCreatedAt());
            return vo;
        }).toList();
    }

    private <T> List<Long> distinct(List<AuditLog> logs, java.util.function.Function<AuditLog, Long> getter) {
        return logs.stream().map(getter).filter(Objects::nonNull).distinct().toList();
    }

    /*
     * 下面两个包装方法存在的唯一理由是：selectBatchIds(空集合) 会拼出
     * "WHERE id IN ( )" 这种非法 SQL，MySQL 直接语法报错。
     * 审计记录里 project_id 本就可以为 null（部门、用户类的变更不属于任何项目），
     * 所以"筛选结果正好全是无项目的记录"是个很容易触发的场景 —— 必须挡住。
     */

    private List<User> selectUsers(List<Long> ids) {
        return ids.isEmpty() ? List.of() : userMapper.selectBatchIds(ids);
    }

    private List<Project> selectProjects(List<Long> ids) {
        return ids.isEmpty() ? List.of() : projectMapper.selectBatchIds(ids);
    }

    private <E> Map<Long, String> nameMap(Collection<E> items,
                                          java.util.function.Function<E, Long> idFn,
                                          java.util.function.Function<E, String> nameFn) {
        return items.stream().collect(Collectors.toMap(idFn, nameFn, (a, b) -> a));
    }
}
