package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysDictType;
import com.erp.system.mapper.SysDictTypeMapper;
import com.erp.system.service.ISysDictTypeService;
import org.springframework.stereotype.Service;

/**
 * 字典类型服务实现
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService {

    @Override
    public SysDictType selectDictTypeByType(String dictType) {
        return getOne(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDict_type, dictType));
    }
}
