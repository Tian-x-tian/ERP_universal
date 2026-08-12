package com.erp.system.logging;

import com.erp.common.core.domain.R;
import com.erp.system.security.service.SecurityUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 控制层请求日志脱敏单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ApiRequestLogAspectTest {

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证请求摘要会对常见敏感字段执行脱敏，避免终端日志泄露业务敏感信息。
     */
    @Test
    void shouldMaskSensitiveFieldsInRequestPayload() {
        ApiRequestLogAspect aspect = new ApiRequestLogAspect(new ObjectMapper(), securityUserResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/system/mdm/customer/save");
        request.setQueryString("contactEmail=tester@example.com&token=plain-token");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contactEmail", "tester@example.com");
        body.put("contactPhone", "13812345678");
        body.put("certNo", "310101199001011234");

        String requestPayload = ReflectionTestUtils.invokeMethod(
                aspect,
                "buildRequestPayload",
                request,
                new Object[]{body});

        Assertions.assertNotNull(requestPayload);
        Assertions.assertTrue(requestPayload.contains("******"));
        Assertions.assertFalse(requestPayload.contains("tester@example.com"));
        Assertions.assertFalse(requestPayload.contains("13812345678"));
        Assertions.assertFalse(requestPayload.contains("310101199001011234"));
        Assertions.assertFalse(requestPayload.contains("plain-token"));
    }

    /**
     * 验证响应摘要同样会对客户联系方式、银行账号等敏感字段执行脱敏。
     */
    @Test
    void shouldMaskSensitiveFieldsInResponsePayload() {
        ApiRequestLogAspect aspect = new ApiRequestLogAspect(new ObjectMapper(), securityUserResolver);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contactEmail", "tester@example.com");
        data.put("contactPhone", "13812345678");
        data.put("bankAccountInfo", "6222021234567890");

        String responsePayload = ReflectionTestUtils.invokeMethod(
                aspect,
                "buildResponsePayload",
                R.success(data));

        Assertions.assertNotNull(responsePayload);
        Assertions.assertTrue(responsePayload.contains("******"));
        Assertions.assertFalse(responsePayload.contains("tester@example.com"));
        Assertions.assertFalse(responsePayload.contains("13812345678"));
        Assertions.assertFalse(responsePayload.contains("6222021234567890"));
    }
}
