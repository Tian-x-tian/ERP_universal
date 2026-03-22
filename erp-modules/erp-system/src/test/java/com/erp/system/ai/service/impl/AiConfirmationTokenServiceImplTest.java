package com.erp.system.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.common.client.internal.InternalSystemClientProperties;
import com.erp.system.ai.model.AiConfirmationPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * AI 确认票据服务测试。
 */
class AiConfirmationTokenServiceImplTest {

    /**
     * 验证票据可正常生成并解析。
     */
    @Test
    void shouldCreateAndParseToken() {
        AiConfirmationTokenServiceImpl tokenService = new AiConfirmationTokenServiceImpl(buildProperties(), new ObjectMapper());
        AiConfirmationPayload payload = buildPayload();

        String token = tokenService.createToken(payload);
        AiConfirmationPayload parsedPayload = tokenService.parseToken(token);

        Assertions.assertEquals(payload.getUserId(), parsedPayload.getUserId());
        Assertions.assertEquals(payload.getTenantId(), parsedPayload.getTenantId());
        Assertions.assertEquals(payload.getActionKey(), parsedPayload.getActionKey());
        Assertions.assertEquals("todoId", parsedPayload.getActionArgs().keySet().iterator().next());
    }

    /**
     * 验证篡改票据会被拒绝。
     */
    @Test
    void shouldRejectTamperedToken() {
        AiConfirmationTokenServiceImpl tokenService = new AiConfirmationTokenServiceImpl(buildProperties(), new ObjectMapper());
        String token = tokenService.createToken(buildPayload());

        String tamperedToken = token + "x";

        Assertions.assertThrows(IllegalArgumentException.class, () -> tokenService.parseToken(tamperedToken));
    }

    private InternalSystemClientProperties buildProperties() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setAuthSignatureSecret("test-secret");
        return properties;
    }

    private AiConfirmationPayload buildPayload() {
        AiConfirmationPayload payload = new AiConfirmationPayload();
        payload.setUserId(1L);
        payload.setTenantId("000000");
        payload.setActionKey("todo_finish");
        payload.setTargetLabel("采购审批 / WF20260322001 / 部门审批");
        payload.setActionArgs(Map.of("todoId", 1L));
        payload.setExpiresAt(System.currentTimeMillis() + 60_000L);
        return payload;
    }
}
