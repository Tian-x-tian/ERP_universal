package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.MdmWarehouseArea;
import com.erp.system.mapper.MdmWarehouseAreaMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmWarehouseAreaService;
import com.erp.system.support.MdmOptimisticLockSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * MDM 仓库库区服务实现。
 */
@Service
public class MdmWarehouseAreaServiceImpl extends ServiceImpl<MdmWarehouseAreaMapper, MdmWarehouseArea>
        implements IMdmWarehouseAreaService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final MdmWarehouseMapper warehouseMapper;
    private final SecurityUserResolver securityUserResolver;

    public MdmWarehouseAreaServiceImpl(MdmWarehouseMapper warehouseMapper, SecurityUserResolver securityUserResolver) {
        this.warehouseMapper = warehouseMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询库区列表。
     *
     * @param warehouseId 仓库ID
     * @param areaCode 库区编码
     * @param areaName 库区名称
     * @param status 状态
     * @return 库区集合
     */
    @Override
    public List<MdmWarehouseArea> selectAreaList(Long warehouseId, String areaCode, String areaName, String status) {
        return list(new LambdaQueryWrapper<MdmWarehouseArea>()
                .eq(MdmWarehouseArea::getDelFlag, DEL_FLAG_EXIST)
                .eq(warehouseId != null, MdmWarehouseArea::getWarehouseId, warehouseId)
                .like(StringUtils.hasText(areaCode), MdmWarehouseArea::getAreaCode, trim(areaCode))
                .like(StringUtils.hasText(areaName), MdmWarehouseArea::getAreaName, trim(areaName))
                .eq(StringUtils.hasText(status), MdmWarehouseArea::getStatus, MdmStatusSupport.normalizeStatus(status))
                .orderByDesc(MdmWarehouseArea::getUpdateTime)
                .orderByDesc(MdmWarehouseArea::getCreateTime));
    }

    /**
     * 新增库区。
     *
     * @param area 库区对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createArea(MdmWarehouseArea area) {
        validateArea(area);
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId) || !existsWarehouse(area.getWarehouseId())) {
            return false;
        }
        if (existsAreaCode(area.getWarehouseId(), area.getAreaCode(), null)) {
            return false;
        }
        Date now = new Date();
        area.setTenantId(tenantId);
        area.setAreaCode(trim(area.getAreaCode()));
        area.setAreaName(trim(area.getAreaName()));
        area.setStatus(MdmStatusSupport.DRAFT);
        area.setVersionNo(1);
        area.setDelFlag(DEL_FLAG_EXIST);
        area.setRemark(MdmValueSupport.trimToNull(area.getRemark()));
        area.setCreateBy(resolveOperator());
        area.setCreateTime(now);
        area.setUpdateBy(resolveOperator());
        area.setUpdateTime(now);
        return save(area);
    }

    /**
     * 修改库区。
     *
     * @param area 库区对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateArea(MdmWarehouseArea area) {
        if (area == null || area.getAreaId() == null) {
            return false;
        }
        validateArea(area);
        MdmWarehouseArea existed = getOne(new LambdaQueryWrapper<MdmWarehouseArea>()
                .eq(MdmWarehouseArea::getAreaId, area.getAreaId())
                .eq(MdmWarehouseArea::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !existsWarehouse(area.getWarehouseId())) {
            return false;
        }
        if (existsAreaCode(area.getWarehouseId(), area.getAreaCode(), area.getAreaId())) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(area.getVersionNo(), existed.getVersionNo(), "库区");
        MdmWarehouseArea updateEntity = new MdmWarehouseArea();
        updateEntity.setAreaId(area.getAreaId());
        updateEntity.setWarehouseId(area.getWarehouseId());
        updateEntity.setAreaCode(trim(area.getAreaCode()));
        updateEntity.setAreaName(trim(area.getAreaName()));
        updateEntity.setStatus(MdmStatusSupport.normalizeStatusForUpdate(area.getStatus(), existed.getStatus()));
        updateEntity.setRemark(MdmValueSupport.trimToNull(area.getRemark()));
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateAreaByVersion(updateEntity, expectedVersion);
    }

    /**
     * 停用库区。
     *
     * @param areaId 库区ID
     * @param versionNo 版本号
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableArea(Long areaId, Integer versionNo) {
        MdmWarehouseArea existed = loadExistingArea(areaId);
        if (existed == null) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "库区");
        MdmWarehouseArea updateEntity = new MdmWarehouseArea();
        updateEntity.setAreaId(areaId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateAreaByVersion(updateEntity, expectedVersion);
    }

    /**
     * 删除库区。
     *
     * @param areaId 库区ID
     * @param versionNo 版本号
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeArea(Long areaId, Integer versionNo) {
        MdmWarehouseArea existed = loadExistingArea(areaId);
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "库区");
        MdmWarehouseArea updateEntity = new MdmWarehouseArea();
        updateEntity.setAreaId(areaId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateAreaByVersion(updateEntity, expectedVersion);
    }

    /**
     * 校验库区基础字段。
     *
     * @param area 库区对象
     */
    private void validateArea(MdmWarehouseArea area) {
        if (area == null || area.getWarehouseId() == null
                || !StringUtils.hasText(area.getAreaCode())
                || !StringUtils.hasText(area.getAreaName())) {
            throw new IllegalArgumentException("仓库、库区编码和库区名称不能为空");
        }
    }

    /**
     * 判断仓库是否存在。
     *
     * @param warehouseId 仓库ID
     * @return true 表示存在
     */
    private boolean existsWarehouse(Long warehouseId) {
        if (warehouseId == null || warehouseId < 1) {
            return false;
        }
        MdmWarehouse warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null && DEL_FLAG_EXIST.equals(warehouse.getDelFlag());
    }

    /**
     * 判断库区编码是否重复。
     *
     * @param warehouseId 仓库ID
     * @param areaCode 库区编码
     * @param excludeId 排除ID
     * @return true 表示重复
     */
    private boolean existsAreaCode(Long warehouseId, String areaCode, Long excludeId) {
        LambdaQueryWrapper<MdmWarehouseArea> queryWrapper = new LambdaQueryWrapper<MdmWarehouseArea>()
                .eq(MdmWarehouseArea::getWarehouseId, warehouseId)
                .eq(MdmWarehouseArea::getAreaCode, trim(areaCode))
                .eq(MdmWarehouseArea::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmWarehouseArea::getAreaId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 根据ID加载有效库区。
     *
     * @param areaId 库区ID
     * @return 库区对象
     */
    private MdmWarehouseArea loadExistingArea(Long areaId) {
        if (areaId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<MdmWarehouseArea>()
                .eq(MdmWarehouseArea::getAreaId, areaId)
                .eq(MdmWarehouseArea::getDelFlag, DEL_FLAG_EXIST));
    }

    /**
     * 使用乐观锁更新库区。
     *
     * @param area 更新对象
     * @param versionNo 期望版本号
     * @return true 表示成功
     */
    private boolean updateAreaByVersion(MdmWarehouseArea area, Integer versionNo) {
        boolean updated = update(area, new LambdaUpdateWrapper<MdmWarehouseArea>()
                .eq(MdmWarehouseArea::getAreaId, area.getAreaId())
                .eq(MdmWarehouseArea::getDelFlag, DEL_FLAG_EXIST)
                .eq(versionNo != null, MdmWarehouseArea::getVersionNo, versionNo));
        MdmOptimisticLockSupport.ensureUpdated(updated, "库区");
        return true;
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }

    /**
     * 裁剪字符串。
     *
     * @param value 原始值
     * @return 标准值
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
