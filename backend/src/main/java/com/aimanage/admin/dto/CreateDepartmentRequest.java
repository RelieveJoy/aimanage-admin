package com.aimanage.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDepartmentRequest {

    @NotBlank(message = "请输入部门名称")
    @Size(max = 50, message = "部门名称过长")
    private String name;

    /** 父节点 ID。树只有两层，因此这里只能填公司根节点的 ID */
    private Long parentId;

    private Integer sort;
}
