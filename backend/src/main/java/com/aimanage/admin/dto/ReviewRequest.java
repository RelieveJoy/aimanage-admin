package com.aimanage.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批意见。批准时可选，<b>拒绝时必填</b>（校验在 Service 层，
 * 因为"拒绝必须给理由"是业务规则而非格式规则）。
 */
@Data
public class ReviewRequest {

    @Size(max = 500, message = "审批意见过长")
    private String comment;
}
