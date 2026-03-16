package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.MdmWarehouseArea;
import com.erp.system.domain.MdmWarehouseLocation;
import com.erp.system.mapper.MdmWarehouseAreaMapper;
import com.erp.system.mapper.MdmWarehouseLocationMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmWarehouseLocationService;
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
 * MDM 仓库库位服务实现。
 */
@Service
public class MdmWarehouseLocationServiceImpl extends ServiceImpl<MdmWarehouseLocationMapper, MdmWarehouseLocation>
        implements IMdmWarehouseLocationService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final MdmWarehouseMapper warehouseMapper;
    private final MdmWarehouseAreaMapper warehouseAreaMapper;
    private final SecurityUserResolver securityUserResolver;

    public MdmWarehouseLocationServiceImpl(MdmWarehouseMapper warehouseMapper,
            MdmWarehouseAreaMapper warehouseAreaMapper,
            SecurityUserResolver securityUserResolver) {
        this.warehouseMapper = warehouseMapper;
        this.warehouseAreaMapper = warehouseAreaMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询库位列表。
     *
     * @param warehouseId 仓库ID
     * @param areaId 库区ID
     * @param locationCode 库位编码
     * @param locationName 库位名称
     * @param status 状态
     * @return 库位集合
     */
    @Override
    public List<MdmWarehouseLocation> selectLocationList(Long warehouseId, Long areaId, String locationCode,
            String locationName, String status) {
        return list(new LambdaQueryWrapper<MdmWarehouseLocation>()
                .eq(MdmWarehouseLocation::getDelFlag, DEL_FLAG_EXIST)
                .eq(warehouseId != null, MdmWarehouseLocation::getWarehouseId, warehouseId)
                .eq(areaId != null, MdmWarehouseLocation::getAreaId, areaId)
                .like(StringUtils.hasText(locationCode), MdmWarehouseLocation::getLocationCode, trim(locationCode))
                .like(StringUtils.hasText(locationName), MdmWarehouseLocation::getLocationName, trim(locationName))
                .eq(StringUtils.hasText(status), MdmWarehouseLocation::getStatus, MdmStatusSupport.normalizeStatus(status))
                .orderByDesc(MdmWarehouseLocation::getUpdateTime)
                .orderByDesc(MdmWarehouseLocation::getCreateTime));
    }

    /**
     * 新增库位。
     *
     * @param location 库位对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createLocation(MdmWarehouseLocation location) {
        validateLocation(location);
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId) || !existsWarehouse(location.getWarehouseId()) || !existsArea(location.getAreaId(), location.getWarehouseId())) {
            return false;
        }
        if (existsLocationCode(location.getWarehouseId(), location.getLocationCode(), null)) {
            return false;
        }
        Date now = new Date();
        location.setTenantId(tenantId);
        location.setLocationCode(trim(location.getLocationCode()));
        location.setLocationName(trim(location.getLocationName()));
        location.setTemperatureZone(MdmValueSupport.trimToNull(location.getTemperatureZone()));
        location.setHazardousFlag(MdmValueSupport.normalizeYN(location.getHazardousFlag(), "N"));
        location.setStatus(MdmStatusSupport.DRAFT);
        location.setVersionNo(1);
        location.setDelFlag(DEL_FLAG_EXIST);
        location.setRemark(MdmValueSupport.trimToNull(location.getRemark()));
        location.setCreateBy(resolveOperator());
        location.setCreateTime(now);
        location.setUpdateBy(resolveOperator());
        location.setUpdateTime(now);
        return save(location);
    }

    /**
     * 修改库位。
     *
     * @param location 库位对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLocation(MdmWarehouseLocation location) {
        if (location == null || location.getLocationId() == null) {
            return false;
        }
        validateLocation(location);
        MdmWarehouseLocation existed = loadExistingLocation(location.getLocationId());
        if (existed == null
                || !existsWarehouse(location.getWarehouseId())
                || !existsArea(location.getAreaId(), location.getWarehouseId())) {
            return false;
        }
        if (existsLocationCode(location.getWarehouseId(), location.getLocationCode(), location.getLocationId())) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(location.getVersionNo(),
                existed.getVersionNo(), "库位");
        MdmWarehouseLocation updateEntity = new MdmWarehouseLocation();
        updateEntity.setLocationId(location.getLocationId());
        updateEntity.setWarehouseId(location.getWarehouseId());
        updateEntity.setAreaId(location.getAreaId());
        updateEntity.setLocationCode(trim(location.getLocationCode()));
        updateEntity.setLocationName(trim(location.getLocationName()));
        updateEntity.setVolumeCapacity(location.getVolumeCapacity());
        updateEntity.setWeightCapacity(location.getWeightCapacity());
        updateEntity.setTemperatureZone(MdmValueSupport.trimToNull(location.getTemperatureZone()));
        updateEntity.setHazardousFlag(MdmValueSupport.normalizeYN(location.getHazardousFlag(), existed.getHazardousFlag()));
        updateEntity.setStatus(MdmStatusSupport.normalizeStatusForUpdate(location.getStatus(), existed.getStatus()));
        updateEntity.setRemark(MdmValueSupport.trimToNull(location.getRemark()));
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateLocationByVersion(updateEntity, expectedVersion);
    }

    /**
     * 停用库位。
     *
     * @param locationId 库位ID
     * @param versionNo 版本号
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableLocation(Long locationId, Integer versionNo) {
        MdmWarehouseLocation existed = loadExistingLocation(locationId);
        if (existed == null) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "库位");
        MdmWarehouseLocation updateEntity = new MdmWarehouseLocation();
        updateEntity.setLocationId(locationId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateLocationByVersion(updateEntity, expectedVersion);
    }

    /**
     * 删除库位。
     *
     * @param locationId 库位ID
     * @param versionNo 版本号
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeLocation(Long locationId, Integer versionNo) {
        MdmWarehouseLocation existed = loadExistingLocation(locationId);
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        Integer expectedVersion = MdmOptimisticLockSupport.requireVersion(versionNo, existed.getVersionNo(), "库位");
        MdmWarehouseLocation updateEntity = new MdmWarehouseLocation();
        updateEntity.setLocationId(locationId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return updateLocationByVersion(updateEntity, expectedVersion);
    }

    /**
     * 校验库位基础字段。
     *
     * @param location 库位对象
     */
    private void validateLocation(MdmWarehouseLocation location) {
        if (location == null || location.getWarehouseId() == null || location.getAreaId() == null
                || !StringUtils.hasText(location.getLocationCode()) || !StringUtils.hasText(location.getLocationName())) {
            throw new IllegalArgumentException("仓库、库区、库位编码和库位名称不能为空");
        }
    }

    /**
     * 判断仓库是否存在。
     *
     * @param warehouseId 仓库ID
     * @return true 表示存在
     */
    private boolean existsWarehouse(Long warehouseId) {
        MdmWarehouse warehouse = warehouseId == null ? null : warehouseMapper.selectById(warehouseId);
        return warehouse != null && DEL_FLAG_EXIST.equals(warehouse.getDelFlag());
    }

    /**
     * 判断库区是否存在且属于仓库。
     *
     * @param areaId 库区ID
     * @param warehouseId 仓库ID
     * @return true 表示存在
     */
    private boolean existsArea(Long areaId, Long warehouseId) {
        MdmWarehouseArea area = areaId == null ? null : warehouseAreaMapper.selectById(areaId);
        return area != null
                && DEL_FLAG_EXIST.equals(area.getDelFlag())
                && warehouseId != null
                && warehouseId.equals(area.getWarehouseId());
    }

    /**
     * 判断库位编码是否重复。
     *
     * @param warehouseId 仓库ID
     * @param locationCode 库位编码
     * @param excludeId 排除ID
     * @return true 表示重复
     */
    private boolean existsLocationCode(Long warehouseId, String locationCode, Long excludeId) {
        LambdaQueryWrapper<MdmWarehouseLocation> queryWrapper = new LambdaQueryWrapper<MdmWarehouseLocation>()
                .eq(MdmWarehouseLocation::getWarehouseId, warehouseId)
                .eq(MdmWarehouseLocation::getLocationCode, trim(locationCode))
                .eq(MdmWarehouseLocation::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmWarehouseLocation::getLocationId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 根据ID加载有效库位。
     *
     * @param locationId 库位ID
     * @return 库位对象
     */
    private MdmWarehouseLocation loadExistingLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<MdmWarehouseLocation>()
                .eq(MdmWarehouseLocation::getLocationId, locationId)
                .eq(MdmWarehouseLocation::getDelFlag, DEL_FLAG_EXIST));
    }

    /**
     * 使用乐观锁更新库位。
     *
     * @param location 更新对象
     * @param versionNo 期望版本号
     * @return true 表示成功
     */
    private boolean updateLocationByVersion(MdmWarehouseLocation location, Integer versionNo) {
        boolean updated = update(location, new LambdaUpdateWrapper<MdmWarehouseLocation>()
                .eq(MdmWarehouseLocation::getLocationId, location.getLocationId())
                .eq(MdmWarehouseLocation::getDelFlag, DEL_FLAG_EXIST)
                .eq(versionNo != null, MdmWarehouseLocation::getVersionNo, versionNo));
        MdmOptimisticLockSupport.ensureUpdated(updated, "库位");
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
