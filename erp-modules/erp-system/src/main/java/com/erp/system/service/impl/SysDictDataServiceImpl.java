package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysDictData;
import com.erp.system.mapper.SysDictDataMapper;
import com.erp.system.service.ISysDictDataService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 字典数据服务实现
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        return list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDict_type, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDict_sort));
    }
}
