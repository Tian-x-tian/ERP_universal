package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmItem;

import java.util.List;

/**
 * MDM 物料主数据服务接口。
 */
public interface IMdmItemService extends IService<MdmItem> {

    /**
     * 查询物料列表。
     *
     * @param itemCode 物料编码
     * @param itemName 物料名称
     * @param status   状态
     * @return 物料列表
     */
    List<MdmItem> selectItemList(String itemCode, String itemName, String status);

    /**
     * 新增物料。
     *
     * @param item 物料对象
     * @return true 表示成功
     */
    boolean createItem(MdmItem item);

    /**
     * 修改物料。
     *
     * @param item 物料对象
     * @return true 表示成功
     */
    boolean updateItem(MdmItem item);

    /**
     * 停用物料。
     *
     * @param itemId 物料ID
     * @return true 表示成功
     */
    boolean disableItem(Long itemId, Integer versionNo);

    /**
     * 删除物料（逻辑删除）。
     *
     * @param itemId 物料ID
     * @return true 表示成功
     */
    boolean removeItem(Long itemId, Integer versionNo);
}
