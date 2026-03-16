package com.erp.business.inventory.support;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 库存值处理辅助工具。
 */
public final class InventoryValueSupport {

    private InventoryValueSupport() {
    }

    /**
     * 将字符串裁剪为空安全值。
     *
     * @param value 原始字符串
     * @return 标准字符串
     */
    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 规范化数量字段。
     *
     * @param value 原始数量
     * @return 非空数量
     */
    public static BigDecimal defaultQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
