package com.erp.system.support;

import org.springframework.util.StringUtils;

/**
 * 状态字段处理工具。
 * 约定：0 表示启用，1 表示停用。
 */
public final class StatusFieldSupport {

    /**
     * 启用状态值。
     */
    public static final String ENABLED = "0";

    /**
     * 停用状态值。
     */
    public static final String DISABLED = "1";

    private StatusFieldSupport() {
    }

    /**
     * 规范二值状态字段（仅允许 0/1）。
     *
     * @param status 原始状态值
     * @return 规范化状态值（0 或 1）
     */
    public static String normalizeBinaryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return ENABLED;
        }
        String normalized = status.trim();
        return DISABLED.equals(normalized) ? DISABLED : ENABLED;
    }

    /**
     * 更新场景下规范二值状态字段。
     * 若前端未传状态值，则保留原状态，避免误改启停状态。
     *
     * @param newStatus     新状态值
     * @param currentStatus 当前状态值
     * @return 规范化后的状态值（0 或 1）
     */
    public static String normalizeBinaryStatusForUpdate(String newStatus, String currentStatus) {
        if (!StringUtils.hasText(newStatus)) {
            return normalizeBinaryStatus(currentStatus);
        }
        return normalizeBinaryStatus(newStatus);
    }

    /**
     * 判断当前状态是否为启用。
     *
     * @param status 状态值
     * @return true 表示启用，false 表示停用
     */
    public static boolean isEnabled(String status) {
        return ENABLED.equals(normalizeBinaryStatus(status));
    }
}
