package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysDictData;
import java.util.List;

/**
 * 字典数据服务接口
 */
public interface ISysDictDataService extends IService<SysDictData> {
    /**
     * 根据字典类型查询字典数据
     * 
     * @param dictType 字典类型
     * @return 字典数据集合
     */
    List<SysDictData> selectDictDataByType(String dictType);
}
