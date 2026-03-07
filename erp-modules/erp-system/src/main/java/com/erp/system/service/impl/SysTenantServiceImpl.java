package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysTenant;
import com.erp.system.mapper.SysTenantMapper;
import com.erp.system.service.ISysTenantService;
import org.springframework.stereotype.Service;

/**
 * 租户服务实现
 */
@Service
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {
}
