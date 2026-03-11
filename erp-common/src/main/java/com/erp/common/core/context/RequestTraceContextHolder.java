package com.erp.common.core.context;

/**
 * 请求链路上下文持有器。
 * 用于在同一次请求内传递 traceId 与请求路径，便于统一响应和日志追踪。
 */
public final class RequestTraceContextHolder {
    private static final ThreadLocal<String> TRACE_ID_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> PATH_CONTEXT = new ThreadLocal<>();

    private RequestTraceContextHolder() {
    }

    /**
     * 写入请求上下文。
     *
     * @param traceId 请求链路标识
     * @param path    请求路径
     */
    public static void setContext(String traceId, String path) {
        TRACE_ID_CONTEXT.set(traceId);
        PATH_CONTEXT.set(path);
    }

    /**
     * 获取当前请求 traceId。
     *
     * @return traceId
     */
    public static String getTraceId() {
        return TRACE_ID_CONTEXT.get();
    }

    /**
     * 获取当前请求路径。
     *
     * @return 请求路径
     */
    public static String getPath() {
        return PATH_CONTEXT.get();
    }

    /**
     * 清理当前线程上下文，避免线程复用污染。
     */
    public static void clear() {
        TRACE_ID_CONTEXT.remove();
        PATH_CONTEXT.remove();
    }
}
