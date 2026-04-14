package com.erp.system.audit;

import com.erp.system.domain.SysOperLog;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysOperLogService;
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
    private ISysOperLogService operLogService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @AfterEach
    void tearDown() {
        com.erp.common.core.context.TenantContextHolder.clear();
    }

    /**
     * 验证普通写接口完成后会写入操作日志。
     *
     * @throws NoSuchMethodException 反射获取测试方法异常
     */
    @Test
    void shouldSaveOperationLogForWriteRequest() throws NoSuchMethodException {
        OperationLogInterceptor interceptor = new OperationLogInterceptor(
                operLogService,
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
        ArgumentCaptor<SysOperLog> logCaptor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(operLogService).save(logCaptor.capture());
        SysOperLog savedLog = logCaptor.getValue();
        Assertions.assertEquals("000001", savedLog.getTenantId());
        Assertions.assertEquals("admin", savedLog.getOperator());
        Assertions.assertEquals("POST", savedLog.getRequestMethod());
        Assertions.assertEquals("/system/user", savedLog.getRequestUri());
        Assertions.assertEquals("127.0.0.1", savedLog.getRequestIp());
        Assertions.assertEquals("1", savedLog.getSuccessFlag());
        Assertions.assertEquals(Integer.valueOf(200), savedLog.getResponseCode());
        Assertions.assertTrue(savedLog.getRequestParams().contains("TestController#save"));
        Assertions.assertTrue(savedLog.getRequestParams().contains("tester"));
        Assertions.assertFalse(savedLog.getRequestParams().contains("tester@example.com"));
        Assertions.assertFalse(savedLog.getRequestParams().contains("6222021234567890"));
        Assertions.assertFalse(savedLog.getRequestParams().contains("13812345678"));
        Assertions.assertFalse(savedLog.getRequestParams().contains("plain-token"));
        Assertions.assertTrue(savedLog.getRequestParams().contains("******"));
    }

    /**
     * 验证登录接口不会写入操作日志。
     *
     * @throws NoSuchMethodException 反射获取测试方法异常
     */
    @Test
    void shouldSkipLoginRequest() throws NoSuchMethodException {
        OperationLogInterceptor interceptor = new OperationLogInterceptor(
                operLogService,
                securityUserResolver,
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
                TestController.class.getMethod("save"));

        boolean continueChain = interceptor.preHandle(request, response, handlerMethod);
        interceptor.afterCompletion(request, response, handlerMethod, null);

        Assertions.assertTrue(continueChain);
        verify(operLogService, never()).save(org.mockito.ArgumentMatchers.any(SysOperLog.class));
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
