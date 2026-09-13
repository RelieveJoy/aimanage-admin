package com.aimanage.admin.dto;

import com.aimanage.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目对外视图。列表页直接展示 PM 姓名与成员数，
 * 满足「明确每个项目的产品经理和成员是谁」这条需求。
 */
@Data
public class ProjectVO {

    private Long id;
    private String name;
    private String code;
    private String description;

    private Long pmId;
    /** 项目经理姓名，避免前端再查一次 */
    private String pmName;

    private Integer status;

    /** 成员总数（含 PM） */
    private Integer memberCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectVO from(Project p) {
        if (p == null) {
            return null;
        }
        ProjectVO vo = new ProjectVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        vo.setDescription(p.getDescription());
        vo.setPmId(p.getPmId());
        vo.setStatus(p.getStatus());
        vo.setCreatedAt(p.getCreatedAt());
        vo.setUpdatedAt(p.getUpdatedAt());
        return vo;
    }
}
