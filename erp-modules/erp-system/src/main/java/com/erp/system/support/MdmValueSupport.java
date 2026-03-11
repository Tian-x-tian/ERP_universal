package com.erp.system.support;

import org.springframework.util.StringUtils;

/**
 * MDM 通用字段处理工具。
 */
public final class MdmValueSupport {

    private MdmValueSupport() {
    }

    /**
     * 去空格并将空字符串转为 null。
     *
     * @param value 原始字符串
     * @return 处理后字符串
     */
    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 去空格并将空字符串转为空串。
     *
     * @param value 原始字符串
     * @return 处理后字符串
     */
    public static String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    /**
     * 计算下一版本号。
     *
     * @param currentVersionNo 当前版本号
     * @return 下一版本号
     */
    public static int resolveNextVersionNo(Integer currentVersionNo) {
        if (currentVersionNo == null || currentVersionNo < 1) {
            return 1;
        }
        return currentVersionNo + 1;
    }

    /**
     * 规范 Y/N 字段。
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 规范值（Y/N）
     */
    public static String normalizeYN(String value, String defaultValue) {
        String normalizedDefault = "Y".equalsIgnoreCase(defaultValue) ? "Y" : "N";
        if (!StringUtils.hasText(value)) {
            return normalizedDefault;
        }
        return "Y".equalsIgnoreCase(value.trim()) ? "Y" : "N";
    }

    /**
     * 税号格式校验：15~20 位数字或大写字母。
     *
     * @param taxNo 税号
     * @return true 表示合法
     */
    public static boolean isValidTaxNo(String taxNo) {
        if (!StringUtils.hasText(taxNo)) {
            return true;
        }
        return taxNo.trim().matches("^[0-9A-Z]{15,20}$");
    }
}
