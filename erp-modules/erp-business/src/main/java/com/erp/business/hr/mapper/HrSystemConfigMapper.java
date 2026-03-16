package com.erp.business.hr.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统参数查询 Mapper。
 */
@Mapper
public interface HrSystemConfigMapper {

    /**
     * 根据参数键名查询参数值。
     *
     * @param configKey 参数键名
     * @return 参数值
     */
    @Select("""
            SELECT config_value
            FROM sys_config
            WHERE config_key = #{configKey}
            ORDER BY config_id DESC
            LIMIT 1
            """)
    String selectConfigValue(@Param("configKey") String configKey);
}
