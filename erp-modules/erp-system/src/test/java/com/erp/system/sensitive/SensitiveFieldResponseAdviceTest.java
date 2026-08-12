package com.erp.system.sensitive;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmCustomer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * 响应脱敏通知单元测试。
 */
class SensitiveFieldResponseAdviceTest {

    /**
     * 验证 MDM 列表响应会对敏感字段执行统一脱敏。
     */
    @Test
    void shouldMaskSensitiveFieldsForMdmResponse() {
        SensitiveFieldResponseAdvice advice = new SensitiveFieldResponseAdvice();
        MdmCustomer customer = new MdmCustomer();
        customer.setTaxNo("913100001234567890");
        customer.setContactPhone("13812345678");
        customer.setContactEmail("tester@example.com");
        R<PageData<MdmCustomer>> response = R.success(PageData.of(List.of(customer), 1, 20, 1));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/system/mdm/customer/list");
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);

        Object masked = advice.beforeBodyWrite(
                response,
                null,
                MediaType.APPLICATION_JSON,
                null,
                new ServletServerHttpRequest(servletRequest),
                serverHttpResponse);

        Assertions.assertSame(response, masked);
        MdmCustomer maskedCustomer = response.getData().getItems().get(0);
        Assertions.assertEquals("91****90", maskedCustomer.getTaxNo());
        Assertions.assertEquals("138****5678", maskedCustomer.getContactPhone());
        Assertions.assertEquals("t****@example.com", maskedCustomer.getContactEmail());
    }
}
