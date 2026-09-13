package com.aimanage.security;

/**
 * 三角色枚举。
 *
 * <p><b>这是三值枚举，不是权限配置表</b> —— 方案 Table 5 将"权限配置页"列为 Won't，
 * 角色规则代码内置。不要把它改成数据库驱动的动态权限。
 *
 * <p>见 docs/admin/ARCHITECTURE.md §1.1。
 */
public enum RoleEnum {

    /** 管平台的人：用户、组织、审计 */
    ADMIN,

    /** 管项目的人：项目、任务、排期 */
    PM,

    /** 干活的人：领任务、改状态、评论 */
    MEMBER;

    public static boolean isValid(String v) {
        if (v == null) {
            return false;
        }
        for (RoleEnum r : values()) {
            if (r.name().equals(v)) {
                return true;
            }
        }
        return false;
    }
}
