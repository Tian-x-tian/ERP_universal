package com.erp.business.inventory.support;

import org.springframework.util.StringUtils;

/**
 * 库存单据状态辅助工具。
 */
public final class InventoryBillStatusSupport {
    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String EXECUTING = "EXECUTING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private InventoryBillStatusSupport() {
    }

    /**
     * 规范化状态值。
     *
     * @param status 原始状态
     * @param defaultStatus 默认状态
     * @return 标准状态
     */
    public static String normalize(String status, String defaultStatus) {
        if (!StringUtils.hasText(status)) {
            return defaultStatus;
        }
        String normalized = status.trim().toUpperCase();
        switch (normalized) {
            case DRAFT:
            case PENDING_APPROVAL:
            case APPROVED:
            case EXECUTING:
            case COMPLETED:
            case CANCELLED:
                return normalized;
            default:
                return defaultStatus;
        }
    }

    /**
     * 判断是否可编辑。
     *
     * @param status 单据状态
     * @return true 表示可编辑
     */
    public static boolean isEditable(String status) {
        return DRAFT.equals(normalize(status, DRAFT));
    }

    /**
     * 判断是否已审批。
     *
     * @param status 单据状态
     * @return true 表示已审批
     */
    public static boolean isApproved(String status) {
        return APPROVED.equals(normalize(status, DRAFT));
    }
}
