package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockTxn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper。
 */
@Mapper
public interface InventoryStockTxnMapper extends BaseMapper<InventoryStockTxn> {
}
