package com.erp.ai.exception;

import com.erp.ai.config.UnifiedResponseBodyAdvice;
import com.erp.ai.filter.TraceIdFilter;
import com.erp.common.core.domain.R;
import com.erp.common.core.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 模块全局异常处理器测试。
 */
class GlobalExceptionHandlerTest {
    private static MockMvc mockMvc;

    @BeforeAll
    static void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new UnifiedResponseBodyAdvice())
                .addFilters(new TraceIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    /**
     * 验证参数校验异常会被统一转换。
     */
    @Test
    void shouldHandleValidationException() throws Exception {
        mockMvc.perform(post("/system/ai/test/validate")
                        .header("X-Trace-Id", "trace-ai-validate-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.traceId").value("trace-ai-validate-001"))
                .andExpect(jsonPath("$.path").value("/system/ai/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证业务异常会被统一转换。
     */
    @Test
    void shouldHandleServiceException() throws Exception {
        mockMvc.perform(get("/system/ai/test/service")
                        .header("X-Trace-Id", "trace-ai-service-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40089))
                .andExpect(jsonPath("$.message").value("AI业务异常"))
                .andExpect(jsonPath("$.traceId").value("trace-ai-service-001"))
                .andExpect(jsonPath("$.path").value("/system/ai/test/service"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证通用异常会被统一转换。
     */
    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/system/ai/test/error")
                        .header("X-Trace-Id", "trace-ai-error-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.traceId").value("trace-ai-error-001"))
                .andExpect(jsonPath("$.path").value("/system/ai/test/error"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 测试控制器。
     */
    @RestController
    static class TestController {

        @PostMapping("/system/ai/test/validate")
        public R<Void> validate(@Valid @RequestBody TestBody body) {
            return R.success();
        }

        @GetMapping("/system/ai/test/service")
        public R<Void> serviceError() {
            throw new ServiceException("AI业务异常", 40089);
        }

        @GetMapping("/system/ai/test/error")
        public R<Void> genericError() {
            throw new RuntimeException("AI系统异常");
        }
    }

    /**
     * 测试请求体。
     */
    static class TestBody {
        @NotBlank(message = "prompt不能为空")
        private String prompt;

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }
}
