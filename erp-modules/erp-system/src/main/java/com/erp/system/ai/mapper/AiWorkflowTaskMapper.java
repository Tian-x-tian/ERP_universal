package com.erp.system.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工作流任务查询 Mapper。
 */
@Mapper
public interface AiWorkflowTaskMapper extends BaseMapper<SysWorkflowTask> {
}
