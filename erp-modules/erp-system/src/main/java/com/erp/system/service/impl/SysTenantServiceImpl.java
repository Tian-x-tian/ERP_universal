package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysTenant;
import com.erp.system.mapper.SysTenantMapper;
import com.erp.system.service.ISysTenantService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 租户服务实现
 */
@Service
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {

    /**
     * 新增租户时规范状态字段和基础审计字段。
     *
     * @param entity 租户实体
     * @return 新增结果
     */
    @Override
    public boolean save(SysTenant entity) {
        normalizeTenant(entity, true, null);
        return super.save(entity);
    }

    /**
     * 修改租户时规范状态字段和更新时间。
     *
     * @param entity 租户实体
     * @return 修改结果
     */
    @Override
    public boolean updateById(SysTenant entity) {
        String currentStatus = null;
        if (entity != null && entity.getId() != null) {
            SysTenant existedTenant = getById(entity.getId());
            currentStatus = existedTenant == null ? null : existedTenant.getStatus();
        }
        normalizeTenant(entity, false, currentStatus);
        return super.updateById(entity);
    }

    /**
     * 规范租户核心字段，避免状态为空导致前端无法判断启停。
     *
     * @param tenant        租户对象
     * @param isCreate      是否为新增操作
     * @param currentStatus 当前已落库状态值（更新场景使用）
     */
    private void normalizeTenant(SysTenant tenant, boolean isCreate, String currentStatus) {
        if (tenant == null) {
            return;
        }
        if (isCreate) {
            tenant.setStatus(StatusFieldSupport.normalizeBinaryStatus(tenant.getStatus()));
        } else {
            tenant.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(tenant.getStatus(), currentStatus));
        }
        if (StringUtils.hasText(tenant.getTenantId())) {
            tenant.setTenantId(tenant.getTenantId().trim());
        }
        if (StringUtils.hasText(tenant.getName())) {
            tenant.setName(tenant.getName().trim());
        }
        if (isCreate) {
            tenant.setCreateTime(new Date());
        } else {
            tenant.setUpdateTime(new Date());
        }
    }
}
