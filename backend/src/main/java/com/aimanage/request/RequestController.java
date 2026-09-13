package com.aimanage.request;

import com.aimanage.common.R;
import com.aimanage.request.dto.SubmitJoinRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 加人申请 · 提交侧（PM 端调用）。
 *
 * <p>路径不带 {@code /admin} 前缀，因此不受管理员拦截规则约束 ——
 * 它由项目经理发起。只有本项目的 PM 能提交，校验在 Service 层。
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public R<Map<String, Long>> submit(@Valid @RequestBody SubmitJoinRequest req) {
        Long id = requestService.submit(req);
        return R.ok(Map.of("id", id));
    }
}
