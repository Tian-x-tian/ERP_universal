package com.erp.ai.config;

import com.erp.common.web.advice.UnifiedResponseBodyAdviceSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * AI 模块统一响应增强器。
 * 负责补齐 traceId、path、timestamp，并根据业务码回写 HTTP 状态。
 */
@RestControllerAdvice
public class UnifiedResponseBodyAdvice extends UnifiedResponseBodyAdviceSupport {
}
