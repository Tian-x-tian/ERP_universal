package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmVersion;
import com.erp.system.mapper.MdmVersionMapper;
import com.erp.system.service.IMdmVersionService;
import org.springframework.stereotype.Service;

/**
 * MDM 版本快照服务实现。
 */
@Service
public class MdmVersionServiceImpl extends ServiceImpl<MdmVersionMapper, MdmVersion> implements IMdmVersionService {
}
