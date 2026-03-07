package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysConfig;
import com.erp.system.mapper.SysConfigMapper;
import com.erp.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

/**
 * 参数配置服务实现
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfig_key, configKey));
        return config != null ? config.getConfig_value() : "";
    }
}
