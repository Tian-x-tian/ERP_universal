package com.erp.system.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.workflow.contract.domain.SysTodoTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 待读取的待办查询 Mapper。
 */
@Mapper
public interface AiTodoTaskMapper extends BaseMapper<SysTodoTask> {
}
