package com.aimanage.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull(message = "请选择要加入的成员")
    private Long userId;
}
