package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单 Mapper。
 */
@Mapper
public interface InventoryOutboundOrderMapper extends BaseMapper<InventoryOutboundOrder> {
}
