package com.aimanage.audit;

import com.aimanage.entity.AuditLog;
import com.aimanage.mapper.AuditLogMapper;
import com.aimanage.security.LoginUser;
import com.aimanage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ★ 审计切面（方案 A2）—— 全链路审计账本的唯一写入方。
 *
 * <p>这是方案里反复强调的那个"AOP 切面自动记录"：
 *
 * <blockquote>
 * "每一次任务状态变更、每一次字段修改，系统通过 AOP 切面自动记录完整链路……
 * 成员不用填一个字，账本自动生成、永久保存、不可篡改。"
 * </blockquote>
 *
 * <p>设计取舍：
 * <ul>
 *   <li><b>切面负责</b>：操作人、角色快照、时间、落库、异常隔离</li>
 *   <li><b>业务方法负责</b>：通过 {@link AuditContext} 补充"改了什么"</li>
 * </ul>
 *
 * <p>审计写入<b>绝不阻塞业务</b>：任何异常都被吞掉并记日志。
 * 账本少一条记录是可接受的降级；因为记日志失败而让用户的写操作失败则不可接受。
 *
 * <p>注意：这是<b>共享后端</b>的基础设施，不属于任何一端。
 * 业务端新增写方法时，加上 {@link Auditable} 注解即可自动纳入审计。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        AuditContext.clear();
        try {
            Object result = pjp.proceed();
            writeLog(pjp, auditable);
            return result;
        } finally {
            AuditContext.clear();
        }
    }

    private void writeLog(ProceedingJoinPoint pjp, Auditable auditable) {
        try {
            LoginUser operator = UserContext.get();
            if (operator == null) {
                // 没有登录上下文（如系统初始化）时不记录，避免出现"无主"的账
                return;
            }

            Object[] args = pjp.getArgs();
            String targetName = AuditContext.targetName();
            if (targetName == null || targetName.isBlank()) {
                targetName = auditable.targetName();
            }

            List<AuditContext.Change> changes = AuditContext.changes();

            if (changes.isEmpty()) {
                // 没有字段级信息（如新增、删除），记一条整体动作
                insert(operator, auditable, args, targetName,
                        null, null, null, AuditContext.remark());
            } else {
                // 有字段级信息：每个字段记一条，这正是竞品缺的"前后值"
                for (AuditContext.Change c : changes) {
                    insert(operator, auditable, args, targetName,
                            c.field(), c.before(), c.after(), AuditContext.remark());
                }
            }
        } catch (Exception e) {
            // 审计失败不能影响业务
            log.error("写入审计账本失败，业务操作已成功但未留痕：" +
                    "type={} action={}", auditable.type(), auditable.action(), e);
        }
    }

    private void insert(LoginUser operator, Auditable auditable, Object[] args,
                        String targetName, String field, String before, String after,
                        String remark) {
        AuditLog logEntry = new AuditLog();
        logEntry.setOperatorId(operator.getUserId());
        // 角色取操作当时的快照 —— 用户之后改角色，历史记录不应跟着变
        logEntry.setOperatorRole(operator.getRole());
        logEntry.setProjectId(argAt(args, auditable.projectIdArg()));
        logEntry.setTargetType(auditable.type());

        // 目标 ID 优先取业务方法指定的（新增场景本次才生成），否则从参数里取
        Long targetId = AuditContext.targetId();
        logEntry.setTargetId(targetId != null ? targetId : argAt(args, auditable.targetIdArg()));
        logEntry.setTargetName(targetName);
        logEntry.setField(field);
        logEntry.setBeforeValue(before);
        logEntry.setAfterValue(after);
        logEntry.setAction(auditable.action());
        logEntry.setRemark(remark);
        auditLogMapper.insert(logEntry);
    }

    /** 取指定位置的 Long 型参数，越界或类型不符时返回 null */
    private Long argAt(Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        Object v = args[index];
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
