package com.erp.common.web.filter;

import com.erp.common.core.context.RequestTraceContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器基类。
 */
public class TraceIdFilterSupport extends OncePerRequestFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final int TRACE_ID_LENGTH = 16;

    /**
     * 处理请求链路追踪上下文。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        String requestPath = request == null ? "" : request.getRequestURI();
        RequestTraceContextHolder.setContext(traceId, requestPath);
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
            RequestTraceContextHolder.clear();
        }
    }

    /**
     * 解析请求 traceId，缺失时自动生成。
     *
     * @param request 请求对象
     * @return traceId
     */
    protected String resolveTraceId(HttpServletRequest request) {
        String traceId = request == null ? null : request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return generateTraceId();
    }

    /**
     * 生成 16 位十六进制 traceId。
     *
     * @return traceId
     */
    protected String generateTraceId() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return raw.length() <= TRACE_ID_LENGTH ? raw : raw.substring(0, TRACE_ID_LENGTH);
    }
}
