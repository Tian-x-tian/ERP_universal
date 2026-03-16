package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryOutboundOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单行 Mapper。
 */
@Mapper
public interface InventoryOutboundOrderLineMapper extends BaseMapper<InventoryOutboundOrderLine> {
}
