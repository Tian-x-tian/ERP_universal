package com.erp.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.ai.model.AiConfirmationPayload;
import com.erp.common.client.internal.InternalSystemClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 动作确认票据服务单元测试。
 */
class AiConfirmationTokenServiceImplTest {

    private AiConfirmationTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        InternalSystemClientProperties properties = new InternalSystemClientProperties();
        properties.setAuthSignatureSecret("ai-confirmation-test-secret");
        tokenService = new AiConfirmationTokenServiceImpl(properties, new ObjectMapper());
    }

    /**
     * 验证签名票据能够完整保留动作上下文。
     */
    @Test
    void shouldCreateAndParseSignedPayload() {
        AiConfirmationPayload payload = validPayload(System.currentTimeMillis() + 60_000L);

        String token = tokenService.createToken(payload);
        AiConfirmationPayload parsedPayload = tokenService.parseToken(token);

        Assertions.assertTrue(token.contains("."));
        Assertions.assertEquals(7L, parsedPayload.getUserId());
        Assertions.assertEquals("tenant-001", parsedPayload.getTenantId());
        Assertions.assertEquals("WORKFLOW_APPROVE", parsedPayload.getActionKey());
        Assertions.assertEquals("采购申请 PR-001", parsedPayload.getTargetLabel());
        Assertions.assertEquals(42, parsedPayload.getActionArgs().get("taskId"));
        Assertions.assertEquals(payload.getExpiresAt(), parsedPayload.getExpiresAt());
    }

    /**
     * 验证票据载荷被篡改后签名校验失败。
     */
    @Test
    void shouldRejectTamperedToken() {
        String token = tokenService.createToken(validPayload(System.currentTimeMillis() + 60_000L));
        char replacement = token.charAt(0) == 'A' ? 'B' : 'A';
        String tamperedToken = replacement + token.substring(1);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.parseToken(tamperedToken));

        Assertions.assertEquals("确认票据签名无效", exception.getMessage());
    }

    /**
     * 验证过期票据不能继续执行 AI 动作。
     */
    @Test
    void shouldRejectExpiredToken() {
        String token = tokenService.createToken(validPayload(System.currentTimeMillis() - 1_000L));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.parseToken(token));

        Assertions.assertEquals("确认票据已过期", exception.getMessage());
    }

    /**
     * 验证缺少关键动作上下文时拒绝生成票据。
     */
    @Test
    void shouldRejectIncompletePayload() {
        AiConfirmationPayload payload = new AiConfirmationPayload();
        payload.setUserId(7L);
        payload.setExpiresAt(System.currentTimeMillis() + 60_000L);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.createToken(payload));

        Assertions.assertEquals("确认票据参数不完整", exception.getMessage());
    }

    private AiConfirmationPayload validPayload(long expiresAt) {
        AiConfirmationPayload payload = new AiConfirmationPayload();
        payload.setUserId(7L);
        payload.setTenantId("tenant-001");
        payload.setActionKey("WORKFLOW_APPROVE");
        payload.setTargetLabel("采购申请 PR-001");
        Map<String, Object> actionArgs = new LinkedHashMap<>();
        actionArgs.put("taskId", 42);
        payload.setActionArgs(actionArgs);
        payload.setExpiresAt(expiresAt);
        return payload;
    }
}
