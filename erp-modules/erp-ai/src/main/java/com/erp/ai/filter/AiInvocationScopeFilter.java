package com.erp.ai.filter;

import com.erp.ai.context.AiInvocationScope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个请求开启 AI 调用作用域缓存。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AiInvocationScopeFilter extends OncePerRequestFilter {

    /**
     * 在请求处理前后开启与关闭调用作用域。
     *
     * @param request     请求
     * @param response    响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        AiInvocationScope.open();
        try {
            filterChain.doFilter(request, response);
        } finally {
            AiInvocationScope.close();
        }
    }
}
