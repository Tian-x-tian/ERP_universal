package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.MdmWarehouseArea;
import com.erp.system.domain.MdmWarehouseLocation;
import com.erp.system.mapper.MdmWarehouseAreaMapper;
import com.erp.system.mapper.MdmWarehouseLocationMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 仓库库位服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MdmWarehouseLocationServiceImplTest {

    @Mock
    private MdmWarehouseLocationMapper warehouseLocationMapper;

    @Mock
    private MdmWarehouseMapper warehouseMapper;

    @Mock
    private MdmWarehouseAreaMapper warehouseAreaMapper;

    private MdmWarehouseLocationServiceImpl warehouseLocationService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("000000");
        warehouseLocationService = new MdmWarehouseLocationServiceImpl(warehouseMapper, warehouseAreaMapper);
        ReflectionTestUtils.setField(warehouseLocationService, "baseMapper", warehouseLocationMapper);
        initTableInfoIfAbsent(MdmWarehouseLocation.class);
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
     * 验证新增库位时会规范危险品标识并写入草稿状态。
     */
    @Test
    void shouldCreateLocationWithNormalizedFlags() {
        MdmWarehouse warehouse = new MdmWarehouse();
        warehouse.setWarehouseId(10L);
        warehouse.setDelFlag("0");
        MdmWarehouseArea area = new MdmWarehouseArea();
        area.setAreaId(11L);
        area.setWarehouseId(10L);
        area.setDelFlag("0");
        MdmWarehouseLocation location = new MdmWarehouseLocation();
        location.setWarehouseId(10L);
        location.setAreaId(11L);
        location.setLocationCode(" L-01 ");
        location.setLocationName(" 常温01 ");
        location.setVolumeCapacity(BigDecimal.TEN);
        location.setWeightCapacity(BigDecimal.ONE);
        location.setHazardousFlag(null);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse);
        when(warehouseAreaMapper.selectById(11L)).thenReturn(area);
        when(warehouseLocationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(warehouseLocationMapper.insert(any(MdmWarehouseLocation.class))).thenReturn(1);

        boolean success = warehouseLocationService.createLocation(location);

        Assertions.assertTrue(success);
        ArgumentCaptor<MdmWarehouseLocation> captor = ArgumentCaptor.forClass(MdmWarehouseLocation.class);
        verify(warehouseLocationMapper).insert(captor.capture());
        Assertions.assertEquals("000000", captor.getValue().getTenantId());
        Assertions.assertEquals("N", captor.getValue().getHazardousFlag());
        Assertions.assertEquals("DRAFT", captor.getValue().getStatus());
    }

    /**
     * 验证停用库位时会按版本号更新状态。
     */
    @Test
    void shouldDisableLocationByVersion() {
        MdmWarehouseLocation existed = new MdmWarehouseLocation();
        existed.setLocationId(30L);
        existed.setStatus("ACTIVE");
        existed.setVersionNo(2);
        existed.setDelFlag("0");
        when(warehouseLocationMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existed);
        when(warehouseLocationMapper.update(any(MdmWarehouseLocation.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = warehouseLocationService.disableLocation(30L, 2);

        Assertions.assertTrue(success);
        ArgumentCaptor<MdmWarehouseLocation> captor = ArgumentCaptor.forClass(MdmWarehouseLocation.class);
        verify(warehouseLocationMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        Assertions.assertEquals("DISABLED", captor.getValue().getStatus());
        Assertions.assertEquals(3, captor.getValue().getVersionNo());
    }
}
