package com.erp.ai.controller;

import com.erp.ai.service.AiBriefService;
import com.erp.common.core.domain.R;
import com.erp.platform.contract.model.PlatformAiBriefView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 每日简报控制层。
 */
@RestController
@RequestMapping("/system/ai/brief")
public class AiBriefController {
    private final AiBriefService aiBriefService;

    public AiBriefController(AiBriefService aiBriefService) {
        this.aiBriefService = aiBriefService;
    }

    /**
     * 查询当日简报，缺失时触发后台生成。
     *
     * @return 简报视图
     */
    @GetMapping
    public R<PlatformAiBriefView> brief() {
        return R.success(aiBriefService.getOrTrigger());
    }

    /**
     * 强制重新生成当日简报。
     *
     * @return 简报视图
     */
    @PostMapping("/refresh")
    public R<PlatformAiBriefView> refresh() {
        return R.success(aiBriefService.refresh());
    }
}
