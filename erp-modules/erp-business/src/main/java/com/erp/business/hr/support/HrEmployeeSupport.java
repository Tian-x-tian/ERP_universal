package com.erp.business.hr.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * HR 员工辅助工具。
 */
public final class HrEmployeeSupport {
    public static final String EXIST_DEL_FLAG = "0";
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LEAVE = "LEAVE";
    public static final String CHANGE_STATUS_DRAFT = "DRAFT";
    public static final String CHANGE_STATUS_SUBMITTED = "SUBMITTED";
    public static final String CHANGE_STATUS_APPROVED = "APPROVED";
    public static final String CHANGE_STATUS_REJECTED = "REJECTED";
    public static final String CHANGE_TYPE_ARCHIVE = "ARCHIVE";
    public static final String CHANGE_TYPE_POSITION = "POSITION";
    public static final String CONTRACT_STATUS_DRAFT = "DRAFT";
    public static final String CONTRACT_STATUS_ACTIVE = "ACTIVE";
    public static final String CONTRACT_STATUS_TERMINATED = "TERMINATED";
    public static final String DOCUMENT_STATUS_ACTIVE = "ACTIVE";
    public static final String DOCUMENT_STATUS_DELETED = "DELETED";
    public static final String WARNING_STATUS_NEW = "NEW";
    public static final String WARNING_STATUS_IGNORED = "IGNORED";
    public static final String WARNING_STATUS_HANDLED = "HANDLED";
    public static final String ATTENDANCE_SYNC_STATUS_PENDING = "PENDING";
    public static final String ATTENDANCE_SYNC_STATUS_SUCCESS = "SUCCESS";
    public static final String ATTENDANCE_SYNC_STATUS_FAILED = "FAILED";
    public static final String ATTENDANCE_SYNC_STATUS_RETRYING = "RETRYING";
    public static final String SALARY_SYNC_STATUS_PENDING = "PENDING";
    public static final String SALARY_SYNC_STATUS_SUCCESS = "SUCCESS";
    public static final String SALARY_SYNC_STATUS_FAILED = "FAILED";
    public static final String SALARY_SYNC_STATUS_RETRYING = "RETRYING";
    public static final String PERFORMANCE_SYNC_STATUS_PENDING = "PENDING";
    public static final String PERFORMANCE_SYNC_STATUS_SUCCESS = "SUCCESS";
    public static final String PERFORMANCE_SYNC_STATUS_FAILED = "FAILED";
    public static final String PERFORMANCE_SYNC_STATUS_RETRYING = "RETRYING";
    public static final String PRIMARY_FLAG_YES = "Y";
    public static final String PRIMARY_FLAG_NO = "N";

    private HrEmployeeSupport() {
    }

    /**
     * 规范化字符串并在空白时返回 null。
     *
     * @param value 原始字符串
     * @return 规范化结果
     */
    public static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 规范化字符串并在空白时返回空字符串。
     *
     * @param value 原始字符串
     * @return 规范化结果
     */
    public static String trimToEmpty(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized;
    }

    /**
     * 掩码手机号。
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        String normalized = trimToNull(mobile);
        if (normalized == null || normalized.length() < 7) {
            return normalized;
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    /**
     * 掩码邮箱。
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        String normalized = trimToNull(email);
        if (normalized == null) {
            return null;
        }
        int separatorIndex = normalized.indexOf('@');
        if (separatorIndex <= 1) {
            return normalized;
        }
        return normalized.charAt(0) + "***" + normalized.substring(separatorIndex);
    }

    /**
     * 掩码证件号。
     *
     * @param certNo 证件号
     * @return 脱敏后的证件号
     */
    public static String maskCertificateNo(String certNo) {
        String normalized = trimToNull(certNo);
        if (normalized == null || normalized.length() <= 4) {
            return normalized;
        }
        int suffixLength = Math.min(4, normalized.length());
        return "****" + normalized.substring(normalized.length() - suffixLength);
    }

    /**
     * 判断证件号唯一校验开关是否开启。
     *
     * @param configValue 配置值
     * @return true 表示开启
     */
    public static boolean isCertUniqueEnabled(String configValue) {
        if (!StringUtils.hasText(configValue)) {
            return true;
        }
        String normalized = configValue.trim().toUpperCase(Locale.ROOT);
        return "1".equals(normalized)
                || "TRUE".equals(normalized)
                || "Y".equals(normalized)
                || "YES".equals(normalized)
                || "ON".equals(normalized);
    }

    /**
     * 规范化页码。
     *
     * @param pageNum 原始页码
     * @return 合法页码
     */
    public static long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化分页大小。
     *
     * @param pageSize 原始页长
     * @return 合法页长
     */
    public static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }

    /**
     * 规范化状态值。
     *
     * @param value 原始状态
     * @return 大写后的状态值
     */
    public static String normalizeStatus(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * 将空字符串转换为默认值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 处理后的字符串
     */
    public static String defaultIfBlank(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    /**
     * 判断两个字符串规范化后是否相等。
     *
     * @param left 左值
     * @param right 右值
     * @return true 表示相等
     */
    public static boolean equalsNormalized(String left, String right) {
        String normalizedLeft = trimToNull(left);
        String normalizedRight = trimToNull(right);
        if (normalizedLeft == null) {
            return normalizedRight == null;
        }
        return normalizedLeft.equals(normalizedRight);
    }
}
