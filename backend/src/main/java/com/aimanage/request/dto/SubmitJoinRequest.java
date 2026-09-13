package com.aimanage.request.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitJoinRequest {

    @NotNull(message = "请选择项目")
    private Long projectId;

    @NotNull(message = "请选择要加入的成员")
    private Long targetUserId;

    @Size(max = 500, message = "申请理由过长")
    private String reason;
}
