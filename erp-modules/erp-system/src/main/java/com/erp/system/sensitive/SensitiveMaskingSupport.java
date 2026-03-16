package com.erp.system.sensitive;

import org.springframework.util.StringUtils;

/**
 * 敏感字段脱敏工具。
 */
public final class SensitiveMaskingSupport {

    private SensitiveMaskingSupport() {
    }

    /**
     * 根据字段类型执行脱敏。
     *
     * @param rawValue 原始值
     * @param type     字段类型
     * @return 脱敏后的值
     */
    public static String mask(String rawValue, SensitiveType type) {
        if (!StringUtils.hasText(rawValue) || type == null) {
            return rawValue;
        }
        String value = rawValue.trim();
        switch (type) {
            case TAX_NO:
                return maskFixed(value, 2, 2);
            case BANK_ACCOUNT:
                return maskFixed(value, 4, 4);
            case PHONE:
                return maskFixed(value, 3, 4);
            case EMAIL:
                return maskEmail(value);
            default:
                return value;
        }
    }

    /**
     * 固定前后保留位脱敏。
     *
     * @param value       原始值
     * @param prefixCount 前缀保留位
     * @param suffixCount 后缀保留位
     * @return 脱敏结果
     */
    private static String maskFixed(String value, int prefixCount, int suffixCount) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.length() <= prefixCount + suffixCount) {
            return "****";
        }
        return value.substring(0, prefixCount) + "****" + value.substring(value.length() - suffixCount);
    }

    /**
     * 邮箱脱敏，保留用户名首字符与域名。
     *
     * @param email 邮箱
     * @return 脱敏结果
     */
    private static String maskEmail(String email) {
        int separatorIndex = email.indexOf('@');
        if (separatorIndex <= 0 || separatorIndex == email.length() - 1) {
            return "****";
        }
        String localPart = email.substring(0, separatorIndex);
        String domainPart = email.substring(separatorIndex);
        if (localPart.length() == 1) {
            return localPart + "****" + domainPart;
        }
        return localPart.substring(0, 1) + "****" + domainPart;
    }
}
