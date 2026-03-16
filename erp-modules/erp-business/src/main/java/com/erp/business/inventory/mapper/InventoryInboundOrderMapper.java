package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单 Mapper。
 */
@Mapper
public interface InventoryInboundOrderMapper extends BaseMapper<InventoryInboundOrder> {
}
