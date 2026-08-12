package com.erp.system.filter;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 鉴权失败统一响应兜底过滤器。
 * 用于补齐极端场景下 401/403 空响应体，确保前后端联调结构稳定。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class UnauthorizedEnvelopeFallbackFilter extends OncePerRequestFilter {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    private final ObjectMapper objectMapper;

    public UnauthorizedEnvelopeFallbackFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 过滤请求并在 401/403 空响应时补齐统一响应体。
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
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, responseWrapper);
            if (needFillUnauthorizedBody(responseWrapper)) {
                fillUnauthorizedBody(responseWrapper, resolveResultCode(responseWrapper.getStatus()));
            }
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * 判断是否需要补齐响应体。
     *
     * @param responseWrapper 响应包装器
     * @return true 表示需要补齐
     */
    private boolean needFillUnauthorizedBody(ContentCachingResponseWrapper responseWrapper) {
        if (responseWrapper == null) {
            return false;
        }
        int status = responseWrapper.getStatus();
        if (status != HttpServletResponse.SC_UNAUTHORIZED && status != HttpServletResponse.SC_FORBIDDEN) {
            return false;
        }
        return responseWrapper.getContentAsByteArray().length == 0;
    }

    /**
     * 根据 HTTP 状态解析业务码。
     *
     * @param status HTTP 状态码
     * @return 业务码枚举
     */
    private ResultCode resolveResultCode(int status) {
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            return ResultCode.FORBIDDEN;
        }
        return ResultCode.UNAUTHORIZED;
    }

    /**
     * 写入统一错误响应。
     *
     * @param responseWrapper 响应包装器
     * @param resultCode      业务码
     * @throws IOException IO 异常
     */
    private void fillUnauthorizedBody(ContentCachingResponseWrapper responseWrapper, ResultCode resultCode)
            throws IOException {
        R<Void> body = R.failed(resultCode);
        byte[] payload = objectMapper.writeValueAsBytes(body);
        responseWrapper.resetBuffer();
        responseWrapper.setCharacterEncoding("UTF-8");
        responseWrapper.setContentType(JSON_CONTENT_TYPE);
        responseWrapper.setContentLength(payload.length);
        responseWrapper.getOutputStream().write(payload);
        responseWrapper.getOutputStream().flush();
    }
}
