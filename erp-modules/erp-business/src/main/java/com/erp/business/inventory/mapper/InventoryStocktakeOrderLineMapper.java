package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStocktakeOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 盘点单行 Mapper。
 */
@Mapper
public interface InventoryStocktakeOrderLineMapper extends BaseMapper<InventoryStocktakeOrderLine> {
}
