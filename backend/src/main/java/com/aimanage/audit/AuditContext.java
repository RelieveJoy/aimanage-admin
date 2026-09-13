package com.aimanage.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务方法向审计切面传递"改了什么"的通道。
 *
 * <p>由 {@link AuditAspect} 在每次被拦截的调用前后清理，业务方法执行期间写入。
 *
 * <p>为什么需要它：操作人、时间、落库这些切面能自己搞定，
 * 但"哪个字段从什么变成了什么"只有业务方法知道 —— 让切面去反推
 * 会写出很脆的反射代码。折中方案是业务方法补一行，切面负责其余全部。
 *
 * <pre>{@code
 * AuditContext.targetName(user.getName());
 * AuditContext.change("role_in_project", null, "MEMBER");
 * }</pre>
 */
public final class AuditContext {

    /** 一条字段级变更 */
    public record Change(String field, String before, String after) {
    }

    private static final ThreadLocal<State> HOLDER = ThreadLocal.withInitial(State::new);

    private static final class State {
        Long projectId;
        Long targetId;
        String targetName;
        String remark;
        final List<Change> changes = new ArrayList<>();
    }

    private AuditContext() {
    }

    /** 记录一次字段变更 */
    public static void change(String field, Object before, Object after) {
        HOLDER.get().changes.add(new Change(field, str(before), str(after)));
    }

    /**
     * 指定所属项目。当 projectId 不在方法参数里时使用，
     * 例如审批场景：项目 ID 来自申请记录，参数列表里只有申请 ID。
     */
    public static void projectId(Long id) {
        HOLDER.get().projectId = id;
    }

    /**
     * 指定目标对象 ID。新增场景必需 —— 此时 ID 是本次插入才生成的，
     * 参数列表里拿不到。
     */
    public static void targetId(Long id) {
        HOLDER.get().targetId = id;
    }

    /** 覆盖目标对象名称（注解里写死的那个会被它替换） */
    public static void targetName(String name) {
        HOLDER.get().targetName = name;
    }

    /** 备注，如"客户方王总电话确认" */
    public static void remark(String remark) {
        HOLDER.get().remark = remark;
    }

    // ---------------- 供 AuditAspect 读取 ----------------

    static Long projectId() {
        return HOLDER.get().projectId;
    }

    static Long targetId() {
        return HOLDER.get().targetId;
    }

    static String targetName() {
        return HOLDER.get().targetName;
    }

    static String remark() {
        return HOLDER.get().remark;
    }

    static List<Change> changes() {
        return HOLDER.get().changes;
    }

    /** 回滚 ThreadLocal，避免线程复用造成串数据 */
    static void clear() {
        HOLDER.remove();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
