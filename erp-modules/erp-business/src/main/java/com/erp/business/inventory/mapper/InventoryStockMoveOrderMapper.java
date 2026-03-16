package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockMoveOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 移库单头 Mapper。
 */
@Mapper
public interface InventoryStockMoveOrderMapper extends BaseMapper<InventoryStockMoveOrder> {
}
