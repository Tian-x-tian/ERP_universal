package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysTodoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程待办任务Mapper接口
 */
@Mapper
public interface SysTodoTaskMapper extends BaseMapper<SysTodoTask> {
}
