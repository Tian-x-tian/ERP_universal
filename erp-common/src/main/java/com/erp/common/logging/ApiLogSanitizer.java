package com.erp.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 接口日志脱敏工具。
 * 统一处理请求参数、查询串与响应对象中的敏感字段，避免日志泄露业务数据。
 */
public class ApiLogSanitizer {
    /**
     * 脱敏后统一占位值。
     */
    public static final String MASKED_VALUE = "******";

    private static final String NULL_VALUE = "null";
    private static final int MAX_COLLECTION_LOG_SIZE = 20;
    private static final int MAX_OBJECT_DEPTH = 4;
    private static final int DEFAULT_STRING_LENGTH = 4000;
    private static final Set<String> SENSITIVE_FIELD_NAMES = new LinkedHashSet<>(Arrays.asList(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "authorization",
            "secret",
            "clientsecret",
            "idtoken",
            "email",
            "mail",
            "phonenumber",
            "phone",
            "mobile",
            "telephone",
            "contactphone",
            "contactemail",
            "emergencyphone",
            "certno",
            "idcard",
            "idnumber",
            "taxno",
            "bankaccount",
            "bankaccountno",
            "bankaccountinfo",
            "callbacktoken"
    ));

    private final ObjectMapper objectMapper;

    public ApiLogSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 对任意对象执行递归脱敏。
     *
     * @param value 原始对象
     * @return 脱敏后的对象
     */
    public Object sanitizeValue(Object value) {
        return sanitizeValue(value, 0, null);
    }

    /**
     * 将查询字符串转换为可日志化的脱敏对象。
     *
     * @param queryString 原始查询串
     * @return 脱敏后的查询对象
     */
    public Object sanitizeQueryString(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return null;
        }
        Map<String, Object> sanitizedQuery = new LinkedHashMap<>();
        String[] segments = queryString.split("&");
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            String[] pair = segment.split("=", 2);
            String rawKey = decode(pair[0]);
            String key = StringUtils.hasText(rawKey) ? rawKey : "unknown";
            String value = pair.length > 1 ? decode(pair[1]) : "";
            Object sanitizedValue = sanitizeValue(value, 1, key);
            mergeQueryValue(sanitizedQuery, key, sanitizedValue);
        }
        return sanitizedQuery;
    }

    /**
     * 对请求参数映射执行脱敏。
     *
     * @param parameterMap 原始请求参数映射
     * @return 脱敏后的参数映射
     */
    public Object sanitizeParameterMap(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitizedParams = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (values == null) {
                sanitizedParams.put(key, null);
                continue;
            }
            if (values.length == 1) {
                sanitizedParams.put(key, sanitizeValue(values[0], 1, key));
                continue;
            }
            sanitizedParams.put(key, sanitizeValue(values, 1, key));
        }
        return sanitizedParams;
    }

    /**
     * 将日志对象写为紧凑 JSON，并按长度限制裁剪。
     *
     * @param payload      日志对象
     * @param maxLogLength 最大日志长度
     * @return JSON 文本
     */
    public String writeCompactJson(Object payload, int maxLogLength) {
        try {
            return truncate(objectMapper.writeValueAsString(payload), maxLogLength);
        } catch (JsonProcessingException ex) {
            return truncate(String.valueOf(payload), maxLogLength);
        }
    }

    /**
     * 裁剪日志文本长度，避免超长日志刷屏。
     *
     * @param text         原始文本
     * @param maxLogLength 最大长度
     * @return 裁剪后的文本
     */
    public String truncate(String text, int maxLogLength) {
        if (!StringUtils.hasText(text)) {
            return NULL_VALUE;
        }
        return text.length() <= maxLogLength ? text : text.substring(0, maxLogLength) + "...[truncated]";
    }

    /**
     * 对任意对象执行递归脱敏与裁剪。
     *
     * @param value     原始对象
     * @param depth     当前递归深度
     * @param fieldName 当前字段名
     * @return 可日志化对象
     */
    @SuppressWarnings("unchecked")
    private Object sanitizeValue(Object value, int depth, String fieldName) {
        if (value == null) {
            return null;
        }
        if (isSensitiveField(fieldName)) {
            return MASKED_VALUE;
        }
        if (depth > MAX_OBJECT_DEPTH) {
            return "[depth-limited]";
        }
        if (value instanceof CharSequence) {
            return truncate(value.toString(), DEFAULT_STRING_LENGTH);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?> || value instanceof Temporal || value instanceof Date) {
            return value;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            int index = 0;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (index >= MAX_COLLECTION_LOG_SIZE) {
                    sanitized.put("_truncated", "size>" + MAX_COLLECTION_LOG_SIZE);
                    break;
                }
                String key = entry.getKey() == null ? "null" : String.valueOf(entry.getKey());
                sanitized.put(key, sanitizeValue(entry.getValue(), depth + 1, key));
                index++;
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>();
            int index = 0;
            for (Object item : collection) {
                if (index >= MAX_COLLECTION_LOG_SIZE) {
                    sanitized.add("[truncated]");
                    break;
                }
                sanitized.add(sanitizeValue(item, depth + 1, fieldName));
                index++;
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>();
            for (int index = 0; index < Math.min(length, MAX_COLLECTION_LOG_SIZE); index++) {
                sanitized.add(sanitizeValue(Array.get(value, index), depth + 1, fieldName));
            }
            if (length > MAX_COLLECTION_LOG_SIZE) {
                sanitized.add("[truncated]");
            }
            return sanitized;
        }
        try {
            Object converted = objectMapper.convertValue(value, Object.class);
            if (converted == value) {
                return truncate(String.valueOf(value), DEFAULT_STRING_LENGTH);
            }
            return sanitizeValue(converted, depth + 1, fieldName);
        } catch (IllegalArgumentException ex) {
            return truncate(String.valueOf(value), DEFAULT_STRING_LENGTH);
        }
    }

    /**
     * 判断字段名是否属于敏感信息字段。
     *
     * @param fieldName 字段名
     * @return true 表示需要脱敏
     */
    private boolean isSensitiveField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalizedFieldName = fieldName.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_NAMES.contains(normalizedFieldName);
    }

    /**
     * 合并查询参数中的重复键值。
     *
     * @param sanitizedQuery 查询映射
     * @param key            查询键
     * @param sanitizedValue 脱敏后的值
     */
    private void mergeQueryValue(Map<String, Object> sanitizedQuery, String key, Object sanitizedValue) {
        Object existingValue = sanitizedQuery.get(key);
        if (existingValue == null) {
            sanitizedQuery.put(key, sanitizedValue);
            return;
        }
        if (existingValue instanceof List<?> existingList) {
            List<Object> merged = new ArrayList<>(existingList);
            merged.add(sanitizedValue);
            sanitizedQuery.put(key, merged);
            return;
        }
        List<Object> merged = new ArrayList<>();
        merged.add(existingValue);
        merged.add(sanitizedValue);
        sanitizedQuery.put(key, merged);
    }

    /**
     * URL 解码查询参数片段；解码失败时返回原值。
     *
     * @param value 原始文本
     * @return 解码后的文本
     */
    private String decode(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }
}
