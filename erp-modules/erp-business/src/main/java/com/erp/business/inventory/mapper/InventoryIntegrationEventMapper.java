package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存集成事件 Mapper。
 */
@Mapper
public interface InventoryIntegrationEventMapper extends BaseMapper<InventoryIntegrationEvent> {
}
