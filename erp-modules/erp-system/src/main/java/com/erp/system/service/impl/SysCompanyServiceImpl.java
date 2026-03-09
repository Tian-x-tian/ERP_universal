package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysCompany;
import com.erp.system.mapper.SysCompanyMapper;
import com.erp.system.service.ISysCompanyService;
import org.springframework.stereotype.Service;

/**
 * 公司服务实现
 */
@Service
public class SysCompanyServiceImpl extends ServiceImpl<SysCompanyMapper, SysCompany> implements ISysCompanyService {
}
