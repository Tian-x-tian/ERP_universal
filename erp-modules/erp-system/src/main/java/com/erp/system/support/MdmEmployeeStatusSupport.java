package com.erp.system.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * MDM 员工状态处理工具。
 */
public final class MdmEmployeeStatusSupport {
    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String ACTIVE = "ACTIVE";
    public static final String LEAVE = "LEAVE";

    private MdmEmployeeStatusSupport() {
    }

    /**
     * 规范员工状态。
     *
     * @param status 原始状态
     * @return 规范化状态
     */
    public static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DRAFT;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (SUBMITTED.equals(normalizedStatus)) {
            return SUBMITTED;
        }
        if (ACTIVE.equals(normalizedStatus)) {
            return ACTIVE;
        }
        if (LEAVE.equals(normalizedStatus)) {
            return LEAVE;
        }
        return DRAFT;
    }

    /**
     * 更新场景规范员工状态。
     *
     * @param newStatus     新状态
     * @param currentStatus 当前状态
     * @return 规范化状态
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
     * @param status 状态
     * @return true 表示草稿
     */
    public static boolean isDraft(String status) {
        return DRAFT.equals(normalizeStatus(status));
    }

    /**
     * 判断是否为审批中状态。
     *
     * @param status 状态
     * @return true 表示审批中
     */
    public static boolean isSubmitted(String status) {
        return SUBMITTED.equals(normalizeStatus(status));
    }

    /**
     * 判断是否为在职状态。
     *
     * @param status 状态
     * @return true 表示在职
     */
    public static boolean isActive(String status) {
        return ACTIVE.equals(normalizeStatus(status));
    }

    /**
     * 判断是否离职状态。
     *
     * @param status 状态
     * @return true 表示离职
     */
    public static boolean isLeave(String status) {
        return LEAVE.equals(normalizeStatus(status));
    }
}
