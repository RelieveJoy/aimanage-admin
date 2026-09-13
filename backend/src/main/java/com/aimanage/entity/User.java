package com.aimanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 {@code sys_user}（见 db/schema.sql）。
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，全局唯一 */
    private String username;

    /** BCrypt 哈希，禁止存明文 */
    private String password;

    /** 真实姓名 */
    private String name;

    /** 取值见 {@link com.aimanage.security.RoleEnum}，存字符串 */
    private String role;

    private Long deptId;

    /** 1 启用 / 0 停用 */
    private Integer status;

    /** 改密码时 +1，使旧 token 失效 */
    private Integer tokenVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
