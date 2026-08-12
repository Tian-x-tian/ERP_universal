package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.InventorySerialRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 序列号记录 Mapper。
 */
@Mapper
public interface InventorySerialRecordMapper extends BaseMapper<InventorySerialRecord> {
}
