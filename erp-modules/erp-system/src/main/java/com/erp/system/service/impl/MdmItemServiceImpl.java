package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmItem;
import com.erp.system.mapper.MdmItemMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmItemService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * MDM 物料主数据服务实现。
 */
@Service
public class MdmItemServiceImpl extends ServiceImpl<MdmItemMapper, MdmItem> implements IMdmItemService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmItemServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询物料列表。
     *
     * @param itemCode 物料编码
     * @param itemName 物料名称
     * @param status   状态
     * @return 物料列表
     */
    @Override
    public List<MdmItem> selectItemList(String itemCode, String itemName, String status) {
        LambdaQueryWrapper<MdmItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmItem::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(itemCode)) {
            queryWrapper.like(MdmItem::getItemCode, itemCode.trim());
        }
        if (StringUtils.hasText(itemName)) {
            queryWrapper.like(MdmItem::getItemName, itemName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmItem::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmItem::getUpdateTime).orderByDesc(MdmItem::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增物料。
     *
     * @param item 物料对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createItem(MdmItem item) {
        if (item == null || !StringUtils.hasText(item.getItemCode())
                || !StringUtils.hasText(item.getItemName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String itemCode = item.getItemCode().trim();
        if (existsItemCode(itemCode, null)) {
            return false;
        }
        String operator = resolveOperator();
        Date now = new Date();
        item.setTenantId(tenantId);
        item.setItemCode(itemCode);
        item.setItemName(item.getItemName().trim());
        item.setSpecModel(MdmValueSupport.trimToNull(item.getSpecModel()));
        item.setBrand(MdmValueSupport.trimToNull(item.getBrand()));
        item.setItemType(MdmValueSupport.trimToNull(item.getItemType()));
        item.setUnitConvert(MdmValueSupport.trimToNull(item.getUnitConvert()));
        item.setBarcode(MdmValueSupport.trimToNull(item.getBarcode()));
        item.setBatchControl(MdmValueSupport.normalizeYN(item.getBatchControl(), "N"));
        item.setSerialControl(MdmValueSupport.normalizeYN(item.getSerialControl(), "N"));
        item.setCostingMethod(MdmValueSupport.trimToNull(item.getCostingMethod()));
        item.setStatus(MdmStatusSupport.normalizeStatus(item.getStatus()));
        item.setVersionNo(1);
        item.setDelFlag(DEL_FLAG_EXIST);
        item.setCreateBy(operator);
        item.setUpdateBy(operator);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        if (MdmStatusSupport.isActive(item.getStatus())) {
            item.setEffectiveTime(now);
        }
        boolean saved = save(item);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.ITEM,
                    item.getItemId(),
                    MdmChangeTypeSupport.CREATE,
                    item.getVersionNo(),
                    item.getStatus(),
                    null,
                    item);
        }
        return saved;
    }

    /**
     * 修改物料。
     *
     * @param item 物料对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItem(MdmItem item) {
        if (item == null || item.getItemId() == null) {
            return false;
        }
        MdmItem existed = getOne(new LambdaQueryWrapper<MdmItem>()
                .eq(MdmItem::getItemId, item.getItemId())
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (StringUtils.hasText(item.getItemCode())
                && !item.getItemCode().trim().equalsIgnoreCase(existed.getItemCode())) {
            return false;
        }
        String newBatchControl = MdmValueSupport.normalizeYN(item.getBatchControl(), existed.getBatchControl());
        String newSerialControl = MdmValueSupport.normalizeYN(item.getSerialControl(), existed.getSerialControl());
        if (Objects.equals(existed.getBatchControl(), "Y") && Objects.equals(newBatchControl, "N")) {
            return false;
        }
        if (Objects.equals(existed.getSerialControl(), "Y") && Objects.equals(newSerialControl, "N")) {
            return false;
        }

        MdmItem before = new MdmItem();
        BeanUtils.copyProperties(existed, before);

        item.setItemName(MdmValueSupport.trimToNull(item.getItemName()));
        item.setSpecModel(MdmValueSupport.trimToNull(item.getSpecModel()));
        item.setBrand(MdmValueSupport.trimToNull(item.getBrand()));
        item.setItemType(MdmValueSupport.trimToNull(item.getItemType()));
        item.setUnitConvert(MdmValueSupport.trimToNull(item.getUnitConvert()));
        item.setBarcode(MdmValueSupport.trimToNull(item.getBarcode()));
        item.setBatchControl(newBatchControl);
        item.setSerialControl(newSerialControl);
        item.setCostingMethod(MdmValueSupport.trimToNull(item.getCostingMethod()));
        String newStatus = MdmStatusSupport.normalizeStatusForUpdate(item.getStatus(), existed.getStatus());
        item.setStatus(newStatus);
        item.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        if (MdmStatusSupport.isActive(newStatus) && existed.getEffectiveTime() == null) {
            item.setEffectiveTime(new Date());
        }
        item.setUpdateBy(resolveOperator());
        item.setUpdateTime(new Date());
        boolean updated = updateById(item);
        if (updated) {
            MdmItem after = getById(item.getItemId());
            auditTrailService.record(MdmDomainTypeSupport.ITEM,
                    item.getItemId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? item.getVersionNo() : after.getVersionNo(),
                    after == null ? item.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用物料。
     *
     * @param itemId 物料ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableItem(Long itemId) {
        if (itemId == null) {
            return false;
        }
        MdmItem existed = getOne(new LambdaQueryWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmItem updateEntity = new MdmItem();
        updateEntity.setItemId(itemId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmItem after = getById(itemId);
            auditTrailService.record(MdmDomainTypeSupport.ITEM,
                    itemId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除物料（逻辑删除）。
     *
     * @param itemId 物料ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeItem(Long itemId) {
        if (itemId == null) {
            return false;
        }
        MdmItem existed = getOne(new LambdaQueryWrapper<MdmItem>()
                .eq(MdmItem::getItemId, itemId)
                .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        MdmItem updateEntity = new MdmItem();
        updateEntity.setItemId(itemId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.ITEM,
                    itemId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断物料编码是否重复。
     *
     * @param itemCode 物料编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsItemCode(String itemCode, Long excludeId) {
        LambdaQueryWrapper<MdmItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmItem::getItemCode, itemCode);
        queryWrapper.eq(MdmItem::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmItem::getItemId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 解析操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }
}
