package com.erp.business.filter;

import com.erp.common.web.filter.TraceIdFilterSupport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 请求链路追踪过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends TraceIdFilterSupport {
}
