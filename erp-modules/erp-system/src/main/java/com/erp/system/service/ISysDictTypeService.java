package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysDictType;
import java.util.List;

/**
 * 字典类型服务接口
 */
public interface ISysDictTypeService extends IService<SysDictType> {
    /**
     * 根据字典类型查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典类型
     */
    SysDictType selectDictTypeByType(String dictType);
}
