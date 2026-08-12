package com.erp.business.config;

import com.erp.common.web.advice.UnifiedResponseBodyAdviceSupport;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一响应增强器。
 */
@RestControllerAdvice
public class UnifiedResponseBodyAdvice extends UnifiedResponseBodyAdviceSupport {
}
