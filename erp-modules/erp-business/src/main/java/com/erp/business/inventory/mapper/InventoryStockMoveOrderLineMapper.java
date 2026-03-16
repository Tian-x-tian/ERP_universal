package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockMoveOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 移库单行 Mapper。
 */
@Mapper
public interface InventoryStockMoveOrderLineMapper extends BaseMapper<InventoryStockMoveOrderLine> {
}
