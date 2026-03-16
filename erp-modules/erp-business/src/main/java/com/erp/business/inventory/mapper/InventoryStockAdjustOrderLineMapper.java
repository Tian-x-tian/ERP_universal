package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockAdjustOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存调整单行 Mapper。
 */
@Mapper
public interface InventoryStockAdjustOrderLineMapper extends BaseMapper<InventoryStockAdjustOrderLine> {
}
