package com.erp.common.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内部调用认证头上下文持有者。
 *
 * <p>内部客户端默认从当前 Servlet 请求里透传调用方的 {@code X-Auth-*} 认证头，
 * 从而让下游按“真实登录用户”而不是合成服务账号来判权。但异步线程（例如 AI 流式对话
 * 线程池）上没有 Servlet 请求绑定，透传会失效并降级成服务主体，导致下游权限判断错位。</p>
 *
 * <p>因此在把任务交给线程池之前，先把认证头快照放进本持有者；
 * {@code InternalRequestHeaderFactory} 在拿不到 Servlet 请求时会回退到这里读取。
 * 任务结束务必调用 {@link #clear()}，避免线程复用造成身份串号。</p>
 */
public final class InternalAuthContextHolder {
    private static final ThreadLocal<Map<String, String>> AUTH_HEADERS = new ThreadLocal<>();

    private InternalAuthContextHolder() {
    }

    /**
     * 绑定当前线程的认证头快照。
     *
     * @param headers 认证头快照，为空表示不绑定
     */
    public static void set(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            AUTH_HEADERS.remove();
            return;
        }
        AUTH_HEADERS.set(Collections.unmodifiableMap(new LinkedHashMap<>(headers)));
    }

    /**
     * 读取当前线程的认证头快照。
     *
     * @return 认证头快照，未绑定时返回空集合
     */
    public static Map<String, String> get() {
        Map<String, String> headers = AUTH_HEADERS.get();
        return headers == null ? Collections.emptyMap() : headers;
    }

    /**
     * 判断当前线程是否已绑定完整的可透传认证头。
     *
     * @return true 表示可以透传
     */
    public static boolean hasForwardableHeaders() {
        Map<String, String> headers = get();
        return hasText(headers.get(AuthHeaders.USER_ID))
                && hasText(headers.get(AuthHeaders.USER_NAME))
                && hasText(headers.get(AuthHeaders.TENANT_ID))
                && hasText(headers.get(AuthHeaders.EXPIRES_AT))
                && hasText(headers.get(AuthHeaders.SIGNATURE));
    }

    /**
     * 清理当前线程绑定。
     */
    public static void clear() {
        AUTH_HEADERS.remove();
    }

    /**
     * 判断字符串是否有内容。
     *
     * @param value 待判断文本
     * @return true 表示非空
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
