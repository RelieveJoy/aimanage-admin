package com.aimanage.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入审计账本的方法（方案 A2）。
 *
 * <p>用法：在 Service 的写方法上加注解，声明"这是什么对象、什么动作、
 * 哪几个参数是项目 ID 和目标 ID"：
 *
 * <pre>{@code
 * @Auditable(type = AuditLog.TARGET_PROJECT_MEMBER, action = AuditLog.ACTION_CREATE,
 *            projectIdArg = 0, targetIdArg = 1)
 * public void addMember(Long projectId, Long userId) { ... }
 * }</pre>
 *
 * <p>操作人、时间、落库、异常兜底由 {@link AuditAspect} 统一处理；
 * 只有"具体改了什么"（字段、前后值）需要业务方法自己通过
 * {@link AuditContext} 补充 —— 因为只有它知道。见 {@link AuditAspect} 的说明。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** 目标对象类型，取值见 {@link com.aimanage.entity.AuditLog} 的 TARGET_* 常量 */
    String type();

    /** 动作，取值见 AuditLog 的 ACTION_* 常量 */
    String action();

    /** 第几个参数是项目 ID（-1 表示没有） */
    int projectIdArg() default -1;

    /** 第几个参数是目标对象 ID（-1 表示没有） */
    int targetIdArg() default -1;

    /** 目标对象名称的默认值，业务方法可用 {@link AuditContext#targetName} 覆盖 */
    String targetName() default "";
}
