package com.erp.auth.exception;

import com.erp.auth.config.UnifiedResponseBodyAdvice;
import com.erp.auth.filter.TraceIdFilter;
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
 * 认证模块全局异常处理器测试。
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
        mockMvc.perform(post("/auth/test/validate")
                        .header("X-Trace-Id", "trace-auth-validate-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.traceId").value("trace-auth-validate-001"))
                .andExpect(jsonPath("$.path").value("/auth/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证业务异常会被统一转换。
     */
    @Test
    void shouldHandleServiceException() throws Exception {
        mockMvc.perform(get("/auth/test/service")
                        .header("X-Trace-Id", "trace-auth-service-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40088))
                .andExpect(jsonPath("$.message").value("认证业务异常"))
                .andExpect(jsonPath("$.traceId").value("trace-auth-service-001"))
                .andExpect(jsonPath("$.path").value("/auth/test/service"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证通用异常会被统一转换。
     */
    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/auth/test/error")
                        .header("X-Trace-Id", "trace-auth-error-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.traceId").value("trace-auth-error-001"))
                .andExpect(jsonPath("$.path").value("/auth/test/error"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 测试控制器。
     */
    @RestController
    static class TestController {

        @PostMapping("/auth/test/validate")
        public R<Void> validate(@Valid @RequestBody TestBody body) {
            return R.success();
        }

        @GetMapping("/auth/test/service")
        public R<Void> serviceError() {
            throw new ServiceException("认证业务异常", 40088);
        }

        @GetMapping("/auth/test/error")
        public R<Void> genericError() {
            throw new RuntimeException("认证系统异常");
        }
    }

    /**
     * 测试请求体。
     */
    static class TestBody {
        @NotBlank(message = "name不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
