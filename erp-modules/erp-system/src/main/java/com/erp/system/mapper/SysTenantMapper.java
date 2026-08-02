package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 租户管理 Mapper 接口
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {
    @Select("SELECT tenant_id FROM sys_tenant WHERE status = '0' AND del_flag = '0' ORDER BY tenant_id")
    List<String> findActiveTenantIds();
}
