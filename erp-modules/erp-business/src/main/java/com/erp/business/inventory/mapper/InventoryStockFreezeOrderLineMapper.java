package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockFreezeOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 冻结解冻单行 Mapper。
 */
@Mapper
public interface InventoryStockFreezeOrderLineMapper extends BaseMapper<InventoryStockFreezeOrderLine> {
}
