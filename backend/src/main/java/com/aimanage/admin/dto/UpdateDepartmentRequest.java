package com.aimanage.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改部门。字段为 null 表示不修改（PATCH 语义）。
 * 注意：<b>不支持改 parentId</b> —— 树只有两层，拖拽改层级属过度设计。
 */
@Data
public class UpdateDepartmentRequest {

    @Size(max = 50, message = "部门名称过长")
    private String name;

    private Integer sort;
}
