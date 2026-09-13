package com.aimanage.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改项目。字段为 null 表示不修改（PATCH 语义）。
 *
 * <p>注意 {@code code} 不可修改 —— 编号是项目的稳定标识，改它没有意义。
 */
@Data
public class UpdateProjectRequest {

    @Size(max = 100, message = "项目名称过长")
    private String name;

    @Size(max = 500, message = "项目描述过长")
    private String description;

    /** 更换项目经理 */
    private Long pmId;

    /** 1 进行中 / 0 已归档 */
    private Integer status;
}
