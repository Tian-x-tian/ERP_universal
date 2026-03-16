package com.erp.business.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.inventory.domain.MdmItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物料主数据只读 Mapper。
 */
@Mapper
public interface MdmItemMapper extends BaseMapper<MdmItem> {
}
