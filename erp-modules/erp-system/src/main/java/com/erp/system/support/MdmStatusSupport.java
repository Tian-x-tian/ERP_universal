package com.erp.system.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * MDM 状态字段处理工具。
 * 约定：
 * DRAFT 表示草稿，ACTIVE 表示生效，DISABLED 表示停用。
 */
public final class MdmStatusSupport {

    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";

    private MdmStatusSupport() {
    }

    /**
     * 规范 MDM 状态值。
     *
     * @param status 原始状态值
     * @return 规范化状态值
     */
    public static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DRAFT;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (ACTIVE.equals(normalizedStatus)) {
            return ACTIVE;
        }
        if (DISABLED.equals(normalizedStatus)) {
            return DISABLED;
        }
        return DRAFT;
    }

    /**
     * 更新场景下规范 MDM 状态值。
     * 若前端未传状态，则沿用旧状态。
     *
     * @param newStatus     新状态值
     * @param currentStatus 当前状态值
     * @return 规范化状态值
     */
    public static String normalizeStatusForUpdate(String newStatus, String currentStatus) {
        if (!StringUtils.hasText(newStatus)) {
            return normalizeStatus(currentStatus);
        }
        return normalizeStatus(newStatus);
    }

    /**
     * 判断是否为草稿状态。
     *
     * @param status 状态值
     * @return true 表示草稿
     */
    public static boolean isDraft(String status) {
        return DRAFT.equals(normalizeStatus(status));
    }

    /**
     * 判断是否为生效状态。
     *
     * @param status 状态值
     * @return true 表示生效
     */
    public static boolean isActive(String status) {
        return ACTIVE.equals(normalizeStatus(status));
    }
}
