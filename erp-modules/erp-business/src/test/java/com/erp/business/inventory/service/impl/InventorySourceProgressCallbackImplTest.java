package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * 来源单进度回写默认实现单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventorySourceProgressCallbackImplTest {

    @Mock
    private IInventoryIntegrationEventService integrationEventService;

    /**
     * 验证默认回写实现会写入来源进度事件。
     */
    @Test
    void shouldRecordSourceProgressEventWhenCallbackInvoked() {
        InventorySourceProgressCallbackImpl callback = new InventorySourceProgressCallbackImpl(integrationEventService);

        Assertions.assertDoesNotThrow(
                () -> callback.callback("PURCHASE_ORDER", 1L, "PO-001", "INB-001", "COMPLETED"));

        verify(integrationEventService).recordSourceProgressEvent("PURCHASE_ORDER", 1L, "PO-001",
                null, null, "INB-001", "COMPLETED");
    }
}
