package com.erp.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.workflow.contract.domain.SysWorkflowTaskAction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程任务动作记录Mapper
 */
@Mapper
public interface SysWorkflowTaskActionMapper extends BaseMapper<SysWorkflowTaskAction> {
}



