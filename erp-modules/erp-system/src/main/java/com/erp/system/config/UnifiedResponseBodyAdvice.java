package com.erp.system.config;

import com.erp.common.web.advice.UnifiedResponseBodyAdviceSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一响应增强器。
 * 负责补全 traceId/path/timestamp，并根据业务码写入 HTTP 状态码。
 */
@RestControllerAdvice
public class UnifiedResponseBodyAdvice extends UnifiedResponseBodyAdviceSupport {
}
