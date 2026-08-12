package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysUserUiPreference;
import com.erp.system.domain.vo.UiPreferenceBundle;

import java.util.List;
import java.util.Map;

/**
 * UI 个性化偏好服务。
 */
public interface ISysUserUiPreferenceService extends IService<SysUserUiPreference> {

    /**
     * 查询当前登录用户的生效 UI 偏好。
     *
     * @return 偏好下发包
     */
    UiPreferenceBundle getCurrentUserPreference();

    /**
     * 保存当前登录用户的个人 UI 偏好，锁定项会被自动剔除。
     *
     * @param preference 个人偏好
     * @return 保存后的偏好下发包
     */
    UiPreferenceBundle saveCurrentUserPreference(Map<String, Object> preference);

    /**
     * 重置当前登录用户的个人 UI 偏好。
     *
     * @return 重置后的偏好下发包
     */
    UiPreferenceBundle resetCurrentUserPreference();

    /**
     * 查询当前租户的默认 UI 策略。
     *
     * @return 偏好下发包
     */
    UiPreferenceBundle getTenantDefault();

    /**
     * 保存当前租户的默认 UI 策略与锁定项。
     *
     * @param preference 租户默认偏好
     * @param lockedKeys 锁定项
     * @return 保存后的偏好下发包
     */
    UiPreferenceBundle saveTenantDefault(Map<String, Object> preference, List<String> lockedKeys);
}
