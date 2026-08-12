package com.erp.ai.filter;

import com.erp.common.web.filter.TraceIdFilterSupport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 模块请求链路追踪过滤器。
 * 负责生成或透传 traceId，并在请求上下文中保存。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends TraceIdFilterSupport {
}
