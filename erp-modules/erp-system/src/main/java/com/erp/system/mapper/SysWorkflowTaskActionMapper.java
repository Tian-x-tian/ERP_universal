package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysWorkflowTaskAction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程任务动作记录Mapper
 */
@Mapper
public interface SysWorkflowTaskActionMapper extends BaseMapper<SysWorkflowTaskAction> {
}

