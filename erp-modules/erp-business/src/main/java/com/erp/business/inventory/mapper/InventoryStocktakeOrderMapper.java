package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点单头 Mapper。
 */
@Mapper
public interface InventoryStocktakeOrderMapper extends BaseMapper<InventoryStocktakeOrder> {
}
