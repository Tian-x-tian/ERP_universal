package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单行 Mapper。
 */
@Mapper
public interface InventoryInboundOrderLineMapper extends BaseMapper<InventoryInboundOrderLine> {
}
