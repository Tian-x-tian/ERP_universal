package com.erp.ai.context;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AI 单次调用作用域缓存。
 *
 * <p>权限判定、租户运行时配置、动作清单这些数据在一次对话里会被反复读取，
 * 而每一次读取都是一次跨服务 HTTP 调用。这里用线程内缓存把同一次调用中的重复读取收敛成一次，
 * 面板场景下并发变高时这个收敛是必需的。</p>
 *
 * <p>作用域必须显式开启和关闭：Servlet 请求由过滤器负责，异步流式任务由线程池装饰器负责。
 * 未开启作用域时 {@link #compute} 直接透传，不做任何缓存，保证语义安全。</p>
 */
public final class AiInvocationScope {
    private static final ThreadLocal<Map<String, Object>> SCOPE = new ThreadLocal<>();
    private static final Object NULL_PLACEHOLDER = new Object();

    private AiInvocationScope() {
    }

    /**
     * 开启当前线程的调用作用域。
     */
    public static void open() {
        SCOPE.set(new HashMap<>());
    }

    /**
     * 关闭当前线程的调用作用域。
     */
    public static void close() {
        SCOPE.remove();
    }

    /**
     * 判断当前线程是否处于调用作用域内。
     *
     * @return true 表示已开启
     */
    public static boolean isActive() {
        return SCOPE.get() != null;
    }

    /**
     * 按缓存键读取值，缺失时通过 supplier 计算并缓存。
     *
     * @param cacheKey 缓存键
     * @param supplier 取值逻辑
     * @param <T>      值类型
     * @return 缓存值或实时计算值
     */
    @SuppressWarnings("unchecked")
    public static <T> T compute(String cacheKey, Supplier<T> supplier) {
        Map<String, Object> scope = SCOPE.get();
        if (scope == null || cacheKey == null) {
            return supplier.get();
        }
        Object cached = scope.get(cacheKey);
        if (cached != null) {
            return cached == NULL_PLACEHOLDER ? null : (T) cached;
        }
        T value = supplier.get();
        scope.put(cacheKey, value == null ? NULL_PLACEHOLDER : value);
        return value;
    }

    /**
     * 主动失效指定缓存键，用于写操作后强制刷新。
     *
     * @param cacheKey 缓存键
     */
    public static void evict(String cacheKey) {
        Map<String, Object> scope = SCOPE.get();
        if (scope != null && cacheKey != null) {
            scope.remove(cacheKey);
        }
    }
}
