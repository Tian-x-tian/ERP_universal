package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI 偏好下发包。
 *
 * <p>前端据此完成三级优先级合并展示：系统默认 ‹ 租户策略 ‹ 用户个人，
 * 其中 {@code lockedKeys} 命中的配置项由租户策略强制生效，用户不可覆盖。
 */
@Data
public class UiPreferenceBundle implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 合并后的生效偏好 */
    private Map<String, Object> preference = new LinkedHashMap<>();

    /** 用户个人偏好原始值 */
    private Map<String, Object> personal = new LinkedHashMap<>();

    /** 租户默认策略 */
    private Map<String, Object> tenantDefault = new LinkedHashMap<>();

    /** 锁定项（用户不可覆盖的配置键） */
    private List<String> lockedKeys = new ArrayList<>();
}
