package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.MdmWarehouseArea;
import com.erp.system.mapper.MdmWarehouseAreaMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.security.service.SecurityUserResolver;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓库库区服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MdmWarehouseAreaServiceImplTest {

    @Mock
    private MdmWarehouseAreaMapper warehouseAreaMapper;

    @Mock
    private MdmWarehouseMapper warehouseMapper;

    @Mock
    private SecurityUserResolver securityUserResolver;

    private MdmWarehouseAreaServiceImpl warehouseAreaService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("000000");
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        warehouseAreaService = new MdmWarehouseAreaServiceImpl(warehouseMapper, securityUserResolver);
        ReflectionTestUtils.setField(warehouseAreaService, "baseMapper", warehouseAreaMapper);
        initTableInfoIfAbsent(MdmWarehouseArea.class);
    }

    /**
     * 清理上下文。
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
     * 验证新增库区会补齐租户、草稿状态和版本号。
     */
    @Test
    void shouldCreateAreaWithDraftDefaults() {
        MdmWarehouse warehouse = new MdmWarehouse();
        warehouse.setWarehouseId(10L);
        warehouse.setDelFlag("0");
        MdmWarehouseArea area = new MdmWarehouseArea();
        area.setWarehouseId(10L);
        area.setAreaCode(" A-01 ");
        area.setAreaName(" 暂存区 ");
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);
        when(warehouseAreaMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(warehouseAreaMapper.insert(any(MdmWarehouseArea.class))).thenReturn(1);

        boolean success = warehouseAreaService.createArea(area);

        Assertions.assertTrue(success);
        ArgumentCaptor<MdmWarehouseArea> captor = ArgumentCaptor.forClass(MdmWarehouseArea.class);
        verify(warehouseAreaMapper).insert(captor.capture());
        Assertions.assertEquals("000000", captor.getValue().getTenantId());
        Assertions.assertEquals("DRAFT", captor.getValue().getStatus());
        Assertions.assertEquals(1, captor.getValue().getVersionNo());
    }

    /**
     * 验证停用库区会按版本号更新状态。
     */
    @Test
    void shouldDisableAreaByVersion() {
        MdmWarehouseArea existed = new MdmWarehouseArea();
        existed.setAreaId(20L);
        existed.setStatus("ACTIVE");
        existed.setVersionNo(3);
        existed.setDelFlag("0");
        when(warehouseAreaMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existed);
        when(warehouseAreaMapper.update(any(MdmWarehouseArea.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = warehouseAreaService.disableArea(20L, 3);

        Assertions.assertTrue(success);
        ArgumentCaptor<MdmWarehouseArea> captor = ArgumentCaptor.forClass(MdmWarehouseArea.class);
        verify(warehouseAreaMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        Assertions.assertEquals("DISABLED", captor.getValue().getStatus());
        Assertions.assertEquals(4, captor.getValue().getVersionNo());
    }
}
