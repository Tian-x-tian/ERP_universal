package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryTransferOrderLine;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调拨单行 Mapper。
 */
@Mapper
public interface InventoryTransferOrderLineMapper extends BaseMapper<InventoryTransferOrderLine> {
}
