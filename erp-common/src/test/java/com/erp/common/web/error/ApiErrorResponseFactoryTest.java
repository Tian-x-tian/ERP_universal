package com.erp.common.web.error;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ApiErrorResponseFactory} 单元测试。
 */
class ApiErrorResponseFactoryTest {

    @AfterEach
    void tearDown() {
        RequestTraceContextHolder.clear();
    }

    /**
     * 验证 Servlet 场景下会复用线程上下文中的 traceId 与 path。
     */
    @Test
    void shouldBuildResponseEntityWithServletTraceContext() {
        RequestTraceContextHolder.setContext("trace-servlet-001", "/auth/test/service");

        ResponseEntity<R<Void>> response = ApiErrorResponseFactory.buildResponseEntity(
                ResultCode.PARAM_ERROR,
                "参数异常");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("参数异常");
        assertThat(response.getBody().getTraceId()).isEqualTo("trace-servlet-001");
        assertThat(response.getBody().getPath()).isEqualTo("/auth/test/service");
        assertThat(response.getBody().getTimestamp()).isNotBlank();
    }

    /**
     * 验证 Reactive 场景下会优先复用传入的 traceId。
     */
    @Test
    void shouldReuseProvidedReactiveTraceId() {
        assertThat(ApiErrorResponseFactory.resolveOrGenerateTraceId("trace-reactive-001"))
                .isEqualTo("trace-reactive-001");
    }

    /**
     * 验证 Reactive 场景缺失 traceId 时会自动生成 16 位十六进制链路标识。
     */
    @Test
    void shouldGenerateReactiveTraceIdWhenMissing() {
        assertThat(ApiErrorResponseFactory.resolveOrGenerateTraceId(null))
                .matches("[0-9a-f]{16}");
    }
}
