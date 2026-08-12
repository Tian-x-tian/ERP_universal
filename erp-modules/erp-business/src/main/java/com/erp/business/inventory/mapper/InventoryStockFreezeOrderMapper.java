package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockFreezeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 冻结解冻单头 Mapper。
 */
@Mapper
public interface InventoryStockFreezeOrderMapper extends BaseMapper<InventoryStockFreezeOrder> {
}
