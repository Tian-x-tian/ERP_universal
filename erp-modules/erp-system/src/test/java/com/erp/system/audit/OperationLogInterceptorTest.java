package com.erp.system.audit;

import com.erp.common.logging.OperationLogPayload;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 操作日志拦截器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogInterceptorTest {

    @Mock
    private OperationLogRecorder operationLogRecorder;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @AfterEach
    void tearDown() {
        com.erp.common.core.context.TenantContextHolder.clear();
    }

    /**
     * 验证普通写接口完成后会记录操作日志，且敏感字段已脱敏。
     *
     * @throws NoSuchMethodException 反射获取测试方法异常
     */
    @Test
    void shouldRecordOperationLogForWriteRequest() throws NoSuchMethodException {
        OperationLogInterceptor interceptor = new OperationLogInterceptor(
                operationLogRecorder,
                securityUserResolver,
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("tenantId", "000001");
        request.addParameter("userName", "tester");
        request.addParameter("contactEmail", "tester@example.com");
        request.addParameter("bankAccountInfo", "6222021234567890");
        request.setQueryString("contactPhone=13812345678&token=plain-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
                TestController.class.getMethod("save"));
        when(securityUserResolver.getCurrentUsername()).thenReturn("admin");

        boolean continueChain = interceptor.preHandle(request, response, handlerMethod);
        interceptor.afterCompletion(request, response, handlerMethod, null);

        Assertions.assertTrue(continueChain);
        ArgumentCaptor<OperationLogPayload> logCaptor = ArgumentCaptor.forClass(OperationLogPayload.class);
        verify(operationLogRecorder).record(logCaptor.capture());
        OperationLogPayload payload = logCaptor.getValue();
        Assertions.assertEquals(OperationLogPayload.TYPE_OPERATION, payload.getLogType());
        Assertions.assertEquals("000001", payload.getTenantId());
        Assertions.assertEquals("admin", payload.getOperator());
        Assertions.assertEquals("POST", payload.getRequestMethod());
        Assertions.assertEquals("/system/user", payload.getRequestUri());
        Assertions.assertEquals("127.0.0.1", payload.getRequestIp());
        Assertions.assertEquals("1", payload.getSuccessFlag());
        Assertions.assertEquals(Integer.valueOf(200), payload.getResponseCode());
        Assertions.assertTrue(payload.getRequestParams().contains("TestController#save"));
        Assertions.assertTrue(payload.getRequestParams().contains("tester"));
        Assertions.assertFalse(payload.getRequestParams().contains("tester@example.com"));
        Assertions.assertFalse(payload.getRequestParams().contains("6222021234567890"));
        Assertions.assertFalse(payload.getRequestParams().contains("13812345678"));
        Assertions.assertFalse(payload.getRequestParams().contains("plain-token"));
        Assertions.assertTrue(payload.getRequestParams().contains("******"));
    }

    /**
     * 验证登录接口不会写入操作日志。
     *
     * @throws NoSuchMethodException 反射获取测试方法异常
     */
    @Test
    void shouldSkipLoginRequest() throws NoSuchMethodException {
        OperationLogInterceptor interceptor = new OperationLogInterceptor(
                operationLogRecorder,
                securityUserResolver,
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
                TestController.class.getMethod("save"));

        boolean continueChain = interceptor.preHandle(request, response, handlerMethod);
        interceptor.afterCompletion(request, response, handlerMethod, null);

        Assertions.assertTrue(continueChain);
        verify(operationLogRecorder, never()).record(org.mockito.ArgumentMatchers.any(OperationLogPayload.class));
    }

    /**
     * 验证日志查询接口自身不会写入操作日志。
     *
     * @throws NoSuchMethodException 反射获取测试方法异常
     */
    @Test
    void shouldSkipOperationLogEndpointItself() throws NoSuchMethodException {
        OperationLogInterceptor interceptor = new OperationLogInterceptor(
                operationLogRecorder,
                securityUserResolver,
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/oper/log/export");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
                TestController.class.getMethod("save"));

        interceptor.preHandle(request, response, handlerMethod);
        interceptor.afterCompletion(request, response, handlerMethod, null);

        verify(operationLogRecorder, never()).record(org.mockito.ArgumentMatchers.any(OperationLogPayload.class));
    }

    /**
     * 测试用控制器。
     */
    private static class TestController {

        /**
         * 空实现，仅用于构造 HandlerMethod。
         */
        public void save() {
        }
    }
}
