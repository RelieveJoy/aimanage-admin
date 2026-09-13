package com.aimanage.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "请输入项目名称")
    @Size(max = 100, message = "项目名称过长")
    private String name;

    @Size(max = 500, message = "项目描述过长")
    private String description;

    /** 必须指定项目经理。编号 code 由系统自动生成，不由前端传入。 */
    @NotNull(message = "请指定项目经理")
    private Long pmId;
}
