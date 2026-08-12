package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventoryTransferOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调拨单头 Mapper。
 */
@Mapper
public interface InventoryTransferOrderMapper extends BaseMapper<InventoryTransferOrder> {
}
