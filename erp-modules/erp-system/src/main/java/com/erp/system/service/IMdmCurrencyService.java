package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmCurrency;

import java.util.List;

/**
 * MDM 币种字典服务接口。
 */
public interface IMdmCurrencyService extends IService<MdmCurrency> {

    /**
     * 查询币种列表。
     *
     * @param currencyCode 币种编码（可选）
     * @param currencyName 币种名称（可选）
     * @param status       状态（可选）
     * @return 币种列表
     */
    List<MdmCurrency> selectList(String currencyCode, String currencyName, String status);

    /**
     * 新增币种。
     *
     * @param currency 币种对象
     * @return true 表示新增成功
     */
    boolean create(MdmCurrency currency);

    /**
     * 修改币种。
     *
     * @param currency 币种对象
     * @return true 表示修改成功
     */
    boolean modify(MdmCurrency currency);

    /**
     * 停用币种。
     *
     * @param currencyId 币种ID
     * @return true 表示停用成功
     */
    boolean disable(Long currencyId);

    /**
     * 删除币种（逻辑删除）。
     *
     * @param currencyId 币种ID
     * @return true 表示删除成功
     */
    boolean remove(Long currencyId);
}
