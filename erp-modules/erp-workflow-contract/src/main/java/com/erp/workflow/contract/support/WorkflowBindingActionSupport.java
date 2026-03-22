package com.erp.workflow.contract.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 流程绑定动作常量。
 */
public final class WorkflowBindingActionSupport {
    public static final String ONBOARD = "ONBOARD";
    public static final String CHANGE = "CHANGE";
    public static final String LEAVE = "LEAVE";

    private WorkflowBindingActionSupport() {
    }

    /**
     * 规范化动作编码。
     *
     * @param actionCode 原始动作编码
     * @return 规范化动作编码
     */
    public static String normalizeAction(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return null;
        }
        return actionCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 判断动作编码是否合法。
     *
     * @param actionCode 动作编码
     * @return true 表示合法
     */
    public static boolean isSupportedAction(String actionCode) {
        String normalized = normalizeAction(actionCode);
        return ONBOARD.equals(normalized) || CHANGE.equals(normalized) || LEAVE.equals(normalized);
    }
}


