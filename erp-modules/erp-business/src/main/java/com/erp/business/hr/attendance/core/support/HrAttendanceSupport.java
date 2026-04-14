package com.erp.business.hr.attendance.core.support;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 出勤模块辅助工具。
 */
public final class HrAttendanceSupport {
    public static final String SOURCE_INTERNAL = "INTERNAL";
    public static final String SOURCE_INTEGRATION = "INTEGRATION";
    public static final String FLAG_YES = "Y";
    public static final String FLAG_NO = "N";
    public static final String RULE_ENABLED = "Y";
    public static final String ORDER_STATUS_DRAFT = "DRAFT";
    public static final String ORDER_STATUS_SUBMITTED = "SUBMITTED";
    public static final String ORDER_STATUS_APPROVED = "APPROVED";
    public static final String ORDER_STATUS_REJECTED = "REJECTED";
    public static final String ORDER_STATUS_WITHDRAWN = "WITHDRAWN";
    public static final String EXCEPTION_LATE = "LATE";
    public static final String EXCEPTION_EARLY_LEAVE = "EARLY_LEAVE";
    public static final String EXCEPTION_MISSING_CARD = "MISSING_CARD";
    public static final String EXCEPTION_ABSENTEEISM = "ABSENTEEISM";
    public static final String EXCEPTION_OUT_OF_RANGE = "OUT_OF_RANGE";
    public static final String LEAVE_TYPE_ANNUAL = "ANNUAL";
    public static final String OVERTIME_TYPE_WORKDAY = "WORKDAY";
    public static final String WORKFLOW_PROCESS_KEY_LEAVE = "hr_attendance_leave";
    public static final String WORKFLOW_PROCESS_KEY_OVERTIME = "hr_attendance_overtime";
    public static final String BUSINESS_TYPE_LEAVE = "HR_ATTENDANCE_LEAVE";
    public static final String BUSINESS_TYPE_OVERTIME = "HR_ATTENDANCE_OVERTIME";
    public static final String OWNER_SERVICE = "business";
    public static final int DEFAULT_RADIUS_METERS = 300;
    public static final LocalTime DEFAULT_SIGN_IN_LIMIT = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_SIGN_OUT_LIMIT = LocalTime.of(18, 0);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter SERIAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private HrAttendanceSupport() {
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
     * 规范化分页页码。
     *
     * @param pageNum 原始页码
     * @return 合法页码
     */
    public static long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化分页页长。
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
     * 将字符串转为统一大写状态值。
     *
     * @param value 原始值
     * @return 大写结果
     */
    public static String normalizeStatus(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * 将日期时间转换为 java.util.Date。
     *
     * @param value 日期时间
     * @return Date 对象
     */
    public static Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Timestamp.valueOf(value);
    }

    /**
     * 将日期转换为 Date。
     *
     * @param value 日期
     * @return Date 对象
     */
    public static Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return java.sql.Date.valueOf(value);
    }

    /**
     * 将 Date 转换为 LocalDate。
     *
     * @param value Date 对象
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 将 Date 转换为 LocalDateTime。
     *
     * @param value Date 对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 规范化月份编码。
     *
     * @param monthCode 月份编码
     * @return 标准月份编码
     */
    public static String normalizeMonthCode(String monthCode) {
        String normalized = trimToNull(monthCode);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() == 7) {
            return normalized;
        }
        if (normalized.length() == 6) {
            return normalized.substring(0, 4) + "-" + normalized.substring(4);
        }
        return normalized;
    }

    /**
     * 从日期生成月份编码。
     *
     * @param workDate 工作日期
     * @return 月份编码
     */
    public static String monthCode(LocalDate workDate) {
        return workDate == null ? null : workDate.format(MONTH_FORMATTER);
    }

    /**
     * 生成业务单号。
     *
     * @param prefix 前缀
     * @param timestamp 参考时间
     * @return 业务单号
     */
    public static String generateOrderNo(String prefix, LocalDateTime timestamp) {
        LocalDateTime safeTimestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        return prefix + safeTimestamp.format(SERIAL_DATE_FORMATTER);
    }

    /**
     * 计算两个坐标之间的直线距离。
     *
     * @param latitude1 坐标一纬度
     * @param longitude1 坐标一经度
     * @param latitude2 坐标二纬度
     * @param longitude2 坐标二经度
     * @return 距离（米）
     */
    public static int calculateDistanceMeters(BigDecimal latitude1, BigDecimal longitude1,
            BigDecimal latitude2, BigDecimal longitude2) {
        if (latitude1 == null || longitude1 == null || latitude2 == null || longitude2 == null) {
            return Integer.MAX_VALUE;
        }
        double earthRadius = 6371000D;
        double latRad1 = Math.toRadians(latitude1.doubleValue());
        double latRad2 = Math.toRadians(latitude2.doubleValue());
        double deltaLat = latRad2 - latRad1;
        double deltaLng = Math.toRadians(longitude2.doubleValue() - longitude1.doubleValue());
        double value = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(latRad1) * Math.cos(latRad2) * Math.pow(Math.sin(deltaLng / 2), 2);
        double distance = 2 * earthRadius * Math.asin(Math.sqrt(value));
        return (int) Math.round(distance);
    }

    /**
     * 计算签到签退之间的工作分钟数。
     *
     * @param signInTime 签到时间
     * @param signOutTime 签退时间
     * @return 工作分钟数
     */
    public static int calculateWorkMinutes(LocalDateTime signInTime, LocalDateTime signOutTime) {
        if (signInTime == null || signOutTime == null || signOutTime.isBefore(signInTime)) {
            return 0;
        }
        return (int) Duration.between(signInTime, signOutTime).toMinutes();
    }

    /**
     * 计算单天范围内的重叠分钟数。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param workDate 工作日期
     * @return 重叠分钟数
     */
    public static int overlapMinutes(Date startTime, Date endTime, LocalDate workDate) {
        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);
        if (start == null || end == null || workDate == null || end.isBefore(start)) {
            return 0;
        }
        LocalDateTime scopeStart = workDate.atStartOfDay();
        LocalDateTime scopeEnd = workDate.plusDays(1).atStartOfDay();
        LocalDateTime effectiveStart = start.isAfter(scopeStart) ? start : scopeStart;
        LocalDateTime effectiveEnd = end.isBefore(scopeEnd) ? end : scopeEnd;
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0;
        }
        return (int) Duration.between(effectiveStart, effectiveEnd).toMinutes();
    }

    /**
     * 将分钟数转换为天数表示，默认按 8 小时工作日折算。
     *
     * @param minutes 分钟数
     * @return 天数
     */
    public static BigDecimal minutesToDays(int minutes) {
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(480), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    /**
     * 在 BigDecimal 为空时返回 0。
     *
     * @param value 原始值
     * @return 非空值
     */
    public static BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
