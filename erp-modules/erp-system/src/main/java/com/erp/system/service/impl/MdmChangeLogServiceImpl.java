package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmChangeLog;
import com.erp.system.mapper.MdmChangeLogMapper;
import com.erp.system.service.IMdmChangeLogService;
import org.springframework.stereotype.Service;

/**
 * MDM 变更日志服务实现。
 */
@Service
public class MdmChangeLogServiceImpl extends ServiceImpl<MdmChangeLogMapper, MdmChangeLog> implements IMdmChangeLogService {
}
