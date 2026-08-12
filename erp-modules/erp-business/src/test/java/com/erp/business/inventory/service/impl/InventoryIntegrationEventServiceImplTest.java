package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.mapper.InventoryIntegrationEventMapper;
import com.erp.business.inventory.service.InventoryFinanceVoucherFacade;
import com.erp.business.inventory.service.InventorySourceIntegrationFacade;
import com.erp.common.core.context.TenantContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存集成事件服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryIntegrationEventServiceImplTest {

    @Mock
    private InventoryIntegrationEventMapper integrationEventMapper;

    @Mock
    private InventorySourceIntegrationFacade sourceIntegrationFacade;

    @Mock
    private InventoryFinanceVoucherFacade financeVoucherFacade;

    private InventoryIntegrationEventServiceImpl integrationEventService;

    /**
     * 初始化测试上下文。
     */
    @BeforeEach
    void setUp() {
        integrationEventService = new InventoryIntegrationEventServiceImpl(integrationEventMapper,
                sourceIntegrationFacade, financeVoucherFacade);
        initTableInfoIfAbsent(InventoryIntegrationEvent.class);
        TenantContextHolder.setTenantId("000000");
    }

    /**
     * 清理租户上下文。
     */
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 初始化实体元数据缓存。
     *
     * @param entityClass 实体类型
     */
    private void initTableInfoIfAbsent(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 验证来源单进度事件会完整写入来源字段。
     */
    @Test
    void shouldInsertSourceProgressEventWithSourceFields() {
        when(integrationEventMapper.insert(any(InventoryIntegrationEvent.class))).thenReturn(1);

        integrationEventService.recordSourceProgressEvent("PURCHASE_ORDER", 101L, "PO-001",
                "PURCHASE_INBOUND", 201L, "IN-001", "COMPLETED");

        ArgumentCaptor<InventoryIntegrationEvent> captor = ArgumentCaptor.forClass(InventoryIntegrationEvent.class);
        verify(integrationEventMapper).insert(captor.capture());
        InventoryIntegrationEvent event = captor.getValue();
        Assertions.assertEquals("000000", event.getTenantId());
        Assertions.assertEquals("SOURCE_PROGRESS", event.getEventType());
        Assertions.assertEquals("PENDING", event.getEventStatus());
        Assertions.assertEquals("PURCHASE_ORDER", event.getSourceType());
        Assertions.assertEquals(101L, event.getSourceId());
        Assertions.assertEquals("PO-001", event.getSourceNo());
        Assertions.assertEquals("PURCHASE_INBOUND", event.getBillType());
        Assertions.assertEquals(201L, event.getBillId());
        Assertions.assertEquals("IN-001", event.getBillNo());
        Assertions.assertTrue(event.getPayloadJson().contains("COMPLETED"));
        // create_by / update_by 已改由 AuditMetaObjectHandlerSupport 自动填充，不再由本服务赋值
    }

    /**
     * 验证重放成功时会回写成功状态与重试次数。
     */
    @Test
    void shouldMarkReplaySuccessWhenSourcePushSucceeds() {
        InventoryIntegrationEvent event = new InventoryIntegrationEvent();
        event.setEventId(1L);
        event.setTenantId("000000");
        event.setEventType("SOURCE_PROGRESS");
        event.setRetryCount(1);
        when(integrationEventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(event);
        when(sourceIntegrationFacade.pushProgress(event)).thenReturn(true);
        when(integrationEventMapper.updateById(any(InventoryIntegrationEvent.class))).thenReturn(1);

        boolean result = integrationEventService.replay(1L);

        Assertions.assertTrue(result);
        ArgumentCaptor<InventoryIntegrationEvent> captor = ArgumentCaptor.forClass(InventoryIntegrationEvent.class);
        verify(integrationEventMapper).updateById(captor.capture());
        Assertions.assertEquals("SUCCESS", captor.getValue().getEventStatus());
        Assertions.assertEquals(2, captor.getValue().getRetryCount());
        Assertions.assertNull(captor.getValue().getLastError());
    }

    /**
     * 财务模块未接入时，凭证事件应归档为 SKIPPED，且不得调用推送、不得记为成功。
     */
    @Test
    void shouldArchiveFinanceVoucherAsSkippedWhenFinanceModuleNotEnabled() {
        InventoryIntegrationEvent event = new InventoryIntegrationEvent();
        event.setEventId(9L);
        event.setTenantId("000000");
        event.setEventType("FINANCE_VOUCHER");
        event.setRetryCount(1);
        when(integrationEventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(event);
        when(financeVoucherFacade.isEnabled()).thenReturn(false);
        when(integrationEventMapper.updateById(any(InventoryIntegrationEvent.class))).thenReturn(1);

        boolean result = integrationEventService.replay(9L);

        Assertions.assertTrue(result);
        ArgumentCaptor<InventoryIntegrationEvent> captor = ArgumentCaptor.forClass(InventoryIntegrationEvent.class);
        verify(integrationEventMapper).updateById(captor.capture());
        Assertions.assertEquals("SKIPPED", captor.getValue().getEventStatus());
        Assertions.assertNotNull(captor.getValue().getLastError());
        // 未接入时不应触碰推送通道，避免留下"投递成功"的假记录
        verify(financeVoucherFacade, never()).pushVoucher(any(InventoryIntegrationEvent.class));
    }
}
