package com.aimanage.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String newPassword;
}
