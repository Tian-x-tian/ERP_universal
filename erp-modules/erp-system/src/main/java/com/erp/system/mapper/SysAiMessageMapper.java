package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysAiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话消息Mapper接口
 */
@Mapper
public interface SysAiMessageMapper extends BaseMapper<SysAiMessage> {
}
