package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockTxnMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存台账服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryLedgerServiceImplTest {

    @Mock
    private InventoryStockBalanceMapper stockBalanceMapper;

    @Mock
    private InventoryStockTxnMapper stockTxnMapper;

    private InventoryLedgerServiceImpl ledgerService;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        ledgerService = new InventoryLedgerServiceImpl(stockBalanceMapper, stockTxnMapper);
        initTableInfoIfAbsent(InventoryStockBalance.class);
        initTableInfoIfAbsent(InventoryStockTxn.class);
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
     * 验证查询余额分页时会透传租户和分页参数。
     */
    @Test
    void shouldSelectBalancePageWithNormalizedPagination() {
        when(stockBalanceMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<InventoryStockBalance> page = invocation.getArgument(0);
            page.setRecords(java.util.Collections.singletonList(new InventoryStockBalance()));
            page.setTotal(1L);
            return page;
        });

        Page<InventoryStockBalance> result = ledgerService.selectBalancePage("TENANT_A", 10L, 20L, 30L, 0L, 500L);

        Assertions.assertEquals(1L, result.getCurrent());
        Assertions.assertEquals(200L, result.getSize());
        Assertions.assertEquals(1, result.getRecords().size());
        ArgumentCaptor<LambdaQueryWrapper<InventoryStockBalance>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(stockBalanceMapper).selectPage(any(Page.class), captor.capture());
        Assertions.assertNotNull(captor.getValue());
    }

    /**
     * 验证查询流水分页时会规范动作类型并执行分页查询。
     */
    @Test
    void shouldSelectTxnPage() {
        when(stockTxnMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<InventoryStockTxn> page = invocation.getArgument(0);
            page.setRecords(java.util.Collections.singletonList(new InventoryStockTxn()));
            page.setTotal(1L);
            return page;
        });

        Page<InventoryStockTxn> result = ledgerService.selectTxnPage("TENANT_A", " INB-001 ", 88L, "outbound", 2L, 50L);

        Assertions.assertEquals(2L, result.getCurrent());
        Assertions.assertEquals(50L, result.getSize());
        Assertions.assertEquals(1, result.getRecords().size());
        verify(stockTxnMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
