package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryStockPolicy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存策略 Mapper。
 */
@Mapper
public interface InventoryStockPolicyMapper extends BaseMapper<InventoryStockPolicy> {
}
