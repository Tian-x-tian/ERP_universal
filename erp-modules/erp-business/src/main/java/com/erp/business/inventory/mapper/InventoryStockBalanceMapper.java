package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存余额 Mapper。
 */
@Mapper
public interface InventoryStockBalanceMapper extends BaseMapper<InventoryStockBalance> {
}
