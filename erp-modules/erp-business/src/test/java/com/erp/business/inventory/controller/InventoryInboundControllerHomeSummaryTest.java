package com.erp.business.inventory.controller;

import com.erp.business.inventory.domain.vo.InventoryInboundHomeSummaryVO;
import com.erp.business.inventory.service.IInventoryHomeSummaryService;
import com.erp.business.inventory.service.IInventoryInboundService;
import com.erp.common.core.domain.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

/**
 * 入库控制器首页汇总接口单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryInboundControllerHomeSummaryTest {

    @Mock
    private IInventoryInboundService inboundService;

    @Mock
    private IInventoryHomeSummaryService inventoryHomeSummaryService;

    /**
     * 验证首页汇总接口返回预期结构。
     */
    @Test
    void shouldReturnInboundHomeSummary() {
        InventoryInboundHomeSummaryVO summaryVO = new InventoryInboundHomeSummaryVO();
        summaryVO.setCurrentMonthInboundQty(new BigDecimal("123.45"));
        when(inventoryHomeSummaryService.buildInboundSummary()).thenReturn(summaryVO);
        InventoryInboundController controller = new InventoryInboundController(inboundService, inventoryHomeSummaryService);

        R<InventoryInboundHomeSummaryVO> response = controller.homeSummary();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals(new BigDecimal("123.45"), response.getData().getCurrentMonthInboundQty());
    }
}
