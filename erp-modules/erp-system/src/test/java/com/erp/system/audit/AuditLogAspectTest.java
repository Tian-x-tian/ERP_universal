package com.erp.system.audit;

import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysAuditLogService;
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
    private ISysAuditLogService auditLogService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证审计日志接口自身查询不会再次写入审计日志。
     */
    @Test
    void shouldSkipAuditLogEndpoints() {
        AuditLogAspect aspect = new AuditLogAspect(auditLogService, securityUserResolver, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/audit/log/list");

        Boolean needRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", request);

        Assertions.assertFalse(Boolean.TRUE.equals(needRecord));
    }

    /**
     * 验证登录日志与主数据追踪接口不会污染审计日志。
     */
    @Test
    void shouldSkipOtherLogEndpoints() {
        AuditLogAspect aspect = new AuditLogAspect(auditLogService, securityUserResolver, new ObjectMapper());
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
        AuditLogAspect aspect = new AuditLogAspect(auditLogService, securityUserResolver, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/user/list");

        Boolean needRecord = ReflectionTestUtils.invokeMethod(aspect, "needRecord", request);

        Assertions.assertTrue(Boolean.TRUE.equals(needRecord));
    }
}
