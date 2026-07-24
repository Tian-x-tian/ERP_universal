package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.vo.UiPreferenceBundle;
import com.erp.system.service.ISysUserUiPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 个性化偏好控制层。
 *
 * <p>个人偏好对所有登录用户开放；租户默认策略与锁定项需要管理权限。
 */
@Tag(name = "UI个性化偏好")
@RestController
@RequestMapping("/system/ui-preference")
@RequiredArgsConstructor
public class SysUiPreferenceController {

    private final ISysUserUiPreferenceService uiPreferenceService;

    @Operation(summary = "查询当前用户生效的UI偏好")
    @GetMapping
    public R<UiPreferenceBundle> getMine() {
        return R.success(uiPreferenceService.getCurrentUserPreference());
    }

    @Operation(summary = "保存当前用户的UI偏好")
    @PutMapping
    public R<UiPreferenceBundle> saveMine(@RequestBody Map<String, Object> preference) {
        return R.success(uiPreferenceService.saveCurrentUserPreference(preference));
    }

    @Operation(summary = "重置当前用户的UI偏好")
    @DeleteMapping
    public R<UiPreferenceBundle> resetMine() {
        return R.success(uiPreferenceService.resetCurrentUserPreference());
    }

    @Operation(summary = "查询租户默认UI策略")
    @PreAuthorize("@ss.hasPermi('system:ui:policy:query')")
    @GetMapping("/tenant-default")
    public R<UiPreferenceBundle> getTenantDefault() {
        return R.success(uiPreferenceService.getTenantDefault());
    }

    @Operation(summary = "保存租户默认UI策略与锁定项")
    @PreAuthorize("@ss.hasPermi('system:ui:policy:edit')")
    @PutMapping("/tenant-default")
    public R<UiPreferenceBundle> saveTenantDefault(@RequestBody TenantDefaultRequest request) {
        Map<String, Object> preference = request == null || request.getPreference() == null
                ? new LinkedHashMap<>()
                : request.getPreference();
        List<String> lockedKeys = request == null || request.getLockedKeys() == null
                ? new ArrayList<>()
                : request.getLockedKeys();
        return R.success(uiPreferenceService.saveTenantDefault(preference, lockedKeys));
    }

    /**
     * 租户默认策略保存请求体。
     */
    @Data
    public static class TenantDefaultRequest {
        /** 租户默认偏好 */
        private Map<String, Object> preference;
        /** 锁定项 */
        private List<String> lockedKeys;
    }
}
