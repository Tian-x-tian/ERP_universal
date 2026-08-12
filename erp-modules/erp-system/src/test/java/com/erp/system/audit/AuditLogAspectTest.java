package com.erp.system.audit;

import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 审计日志切面单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private OperationLogRecorder operationLogRecorder;

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证审计日志接口自身查询不会再次写入审计日志。
     */
    @Test
    void shouldSkipAuditLogEndpoints() {
        AuditLogAspect aspect = newAspect();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/audit/log/list");

        Boolean needRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", request);

        Assertions.assertFalse(Boolean.TRUE.equals(needRecord));
    }

    /**
     * 验证登录日志与主数据追踪接口不会污染审计日志。
     */
    @Test
    void shouldSkipOtherLogEndpoints() {
        AuditLogAspect aspect = newAspect();
        MockHttpServletRequest loginLogRequest = new MockHttpServletRequest("GET", "/system/login/log/list");
        MockHttpServletRequest mdmTraceRequest = new MockHttpServletRequest("GET", "/system/mdm/trace/log/list");

        Boolean loginNeedRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", loginLogRequest);
        Boolean traceNeedRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", mdmTraceRequest);

        Assertions.assertFalse(Boolean.TRUE.equals(loginNeedRecord));
        Assertions.assertFalse(Boolean.TRUE.equals(traceNeedRecord));
    }

    /**
     * 验证普通查询接口仍会正常进入审计日志采集。
     */
    @Test
    void shouldRecordRegularQueryEndpoint() {
        AuditLogAspect aspect = newAspect();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/list");

        Boolean needRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", request);

        Assertions.assertTrue(Boolean.TRUE.equals(needRecord));
    }

    /**
     * 验证写操作不会进入审计日志，由操作日志拦截器单独记录。
     */
    @Test
    void shouldSkipWriteRequest() {
        AuditLogAspect aspect = newAspect();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/user");

        Boolean needRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", request);

        Assertions.assertFalse(Boolean.TRUE.equals(needRecord));
    }

    /**
     * 构建待测切面。
     *
     * @return 审计日志切面
     */
    private AuditLogAspect newAspect() {
        return new AuditLogAspect(operationLogRecorder, securityUserResolver, new ObjectMapper());
    }
}
