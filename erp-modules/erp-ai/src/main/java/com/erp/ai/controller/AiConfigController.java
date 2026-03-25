package com.erp.ai.controller;

import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 配置中心控制层。
 */
@RestController
@RequestMapping("/system/ai")
public class AiConfigController {
    private final AiTenantConfigService aiTenantConfigService;

    public AiConfigController(AiTenantConfigService aiTenantConfigService) {
        this.aiTenantConfigService = aiTenantConfigService;
    }

    /**
     * 查询当前租户 AI 配置。
     *
     * @return AI 配置
     */
    @GetMapping("/config")
    public R<PlatformAiConfigView> config() {
        if (!hasAnyPermission("system:ai:config:list", "system:ai:config:query")) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(aiTenantConfigService.getConfig());
    }

    /**
     * 更新当前租户 AI 配置。
     *
     * @param request 更新请求
     * @return 更新后的配置
     */
    @PutMapping("/config")
    public R<PlatformAiConfigView> updateConfig(@RequestBody(required = false) PlatformAiConfigUpdateRequest request) {
        if (!aiTenantConfigService.hasPermission("system:ai:config:edit")) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(aiTenantConfigService.updateConfig(request));
    }

    /**
     * 查询动作策略。
     *
     * @return 动作策略列表
     */
    @GetMapping("/policy/actions")
    public R<List<PlatformAiActionPolicyItem>> policyActions() {
        if (!hasAnyPermission("system:ai:config:list", "system:ai:config:query", "system:ai:policy:edit")) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(aiTenantConfigService.listActionPolicies());
    }

    /**
     * 更新动作策略。
     *
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    @PutMapping("/policy/actions")
    public R<List<PlatformAiActionPolicyItem>> updatePolicyActions(
            @RequestBody(required = false) List<PlatformAiActionPolicyItem> policyItems) {
        if (!aiTenantConfigService.hasPermission("system:ai:policy:edit")) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(aiTenantConfigService.updateActionPolicies(policyItems));
    }

    /**
     * 查询 AI 审计记录。
     *
     * @param limit 查询条数
     * @return 审计记录列表
     */
    @GetMapping("/audit")
    public R<List<PlatformAiAuditView>> audit(@RequestParam(value = "limit", required = false) Integer limit) {
        if (!hasAnyPermission("system:ai:audit:list", "system:ai:ops:view")) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        int safeLimit = limit == null ? 50 : limit;
        return R.success(aiTenantConfigService.listAuditRecords(safeLimit));
    }

    /**
     * 判断是否拥有任一权限。
     *
     * @param permissions 权限集合
     * @return true 表示拥有任一权限
     */
    private boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        for (String permission : permissions) {
            if (aiTenantConfigService.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
