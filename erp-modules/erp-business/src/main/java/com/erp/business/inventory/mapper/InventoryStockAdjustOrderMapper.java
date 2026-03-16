package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockAdjustOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存调整单头 Mapper。
 */
@Mapper
public interface InventoryStockAdjustOrderMapper extends BaseMapper<InventoryStockAdjustOrder> {
}
