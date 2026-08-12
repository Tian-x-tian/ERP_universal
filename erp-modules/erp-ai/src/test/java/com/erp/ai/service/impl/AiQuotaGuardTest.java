package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiQuotaGuardTest {
    private static final Instant NOW = Instant.parse("2026-08-02T04:00:00Z");
    private final InternalSystemClient systemClient = mock(InternalSystemClient.class);
    private final ErpAiProperties properties = new ErpAiProperties();
    private AiQuotaGuard quotaGuard;

    @BeforeEach
    void setUp() {
        properties.setMaxInputTokens(32768);
        properties.setMaxOutputTokens(4096);
        quotaGuard = new AiQuotaGuard(systemClient, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReserveAndSettleInputOutputInAtomicBatches() {
        AiQuotaReservation reservation = quotaGuard.reserve(120L);
        org.mockito.ArgumentCaptor<List<SaasUsageEvent>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(systemClient).applyQuotaEvents(captor.capture());
        assertThat(captor.getValue()).extracting(SaasUsageEvent::getOperation)
                .containsOnly(SaasUsageOperation.RESERVE);
        assertThat(captor.getValue()).extracting(SaasUsageEvent::getMetricKey)
                .containsExactly(SaasQuotaKeys.AI_INPUT_TOKENS, SaasQuotaKeys.AI_OUTPUT_TOKENS);
        assertThat(captor.getValue()).extracting(SaasUsageEvent::getAmount)
                .containsExactly(120L, 4096L);

        quotaGuard.settle(reservation, 90L, 12L, true);

        verify(systemClient, org.mockito.Mockito.times(2)).applyQuotaEvents(captor.capture());
        List<SaasUsageEvent> settled = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(settled).extracting(SaasUsageEvent::getOperation).containsOnly(SaasUsageOperation.SETTLE);
        assertThat(settled).extracting(SaasUsageEvent::getAmount).containsExactly(90L, 12L);
    }

    @Test
    void shouldUseReservedMaximumWhenSuccessfulResponseHasNoUsage() {
        AiQuotaReservation reservation = quotaGuard.reserve(120L);

        quotaGuard.settle(reservation, null, null, false);

        org.mockito.ArgumentCaptor<List<SaasUsageEvent>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(systemClient, org.mockito.Mockito.times(2)).applyQuotaEvents(captor.capture());
        assertThat(captor.getAllValues().get(1)).extracting(SaasUsageEvent::getAmount)
                .containsExactly(120L, 4096L);
    }

    @Test
    void shouldReleaseBothReservationsForFailedRequest() {
        AiQuotaReservation reservation = quotaGuard.reserve(40L);

        quotaGuard.release(reservation);

        org.mockito.ArgumentCaptor<List<SaasUsageEvent>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(systemClient, org.mockito.Mockito.times(2)).applyQuotaEvents(captor.capture());
        assertThat(captor.getAllValues().get(1)).extracting(SaasUsageEvent::getOperation)
                .containsOnly(SaasUsageOperation.RELEASE);
        assertThat(captor.getAllValues().get(1)).extracting(SaasUsageEvent::getAmount)
                .containsOnlyNulls();
    }
}
