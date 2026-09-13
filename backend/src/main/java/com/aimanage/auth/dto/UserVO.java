package com.aimanage.auth.dto;

import com.aimanage.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对外视图，<b>不含 password</b>。
 *
 * <p>所有返回用户信息的接口都应经由此类，避免密码哈希泄露。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String name;
    private String role;
    private Long deptId;
    private String deptName;
    private Integer status;
    private LocalDateTime createdAt;

    public static UserVO from(User u) {
        if (u == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setName(u.getName());
        vo.setRole(u.getRole());
        vo.setDeptId(u.getDeptId());
        vo.setStatus(u.getStatus());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }
}
