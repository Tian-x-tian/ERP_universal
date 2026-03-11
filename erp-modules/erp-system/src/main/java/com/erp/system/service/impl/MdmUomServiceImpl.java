package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmUom;
import com.erp.system.mapper.MdmUomMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmUomService;
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
import java.util.Locale;

/**
 * MDM 计量单位字典服务实现。
 */
@Service
public class MdmUomServiceImpl extends ServiceImpl<MdmUomMapper, MdmUom> implements IMdmUomService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmUomServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询计量单位列表。
     *
     * @param uomCode 单位编码
     * @param uomName 单位名称
     * @param status 状态
     * @return 计量单位列表
     */
    @Override
    public List<MdmUom> selectList(String uomCode, String uomName, String status) {
        LambdaQueryWrapper<MdmUom> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmUom::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(uomCode)) {
            queryWrapper.like(MdmUom::getUomCode, uomCode.trim());
        }
        if (StringUtils.hasText(uomName)) {
            queryWrapper.like(MdmUom::getUomName, uomName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmUom::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmUom::getUpdateTime).orderByDesc(MdmUom::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增计量单位。
     *
     * @param uom 计量单位对象
     * @return true 表示新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(MdmUom uom) {
        if (uom == null || !StringUtils.hasText(uom.getUomCode()) || !StringUtils.hasText(uom.getUomName())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String uomCode = normalizeCode(uom.getUomCode());
        if (existsCode(uomCode, null)) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        uom.setTenantId(tenantId);
        uom.setUomCode(uomCode);
        uom.setUomName(uom.getUomName().trim());
        uom.setBaseUomCode(normalizeCode(uom.getBaseUomCode()));
        uom.setStatus(MdmStatusSupport.normalizeStatus(uom.getStatus()));
        uom.setVersionNo(1);
        uom.setDelFlag(DEL_FLAG_EXIST);
        uom.setCreateBy(operator);
        uom.setUpdateBy(operator);
        uom.setCreateTime(now);
        uom.setUpdateTime(now);
        boolean saved = save(uom);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.UOM,
                    uom.getUomId(),
                    MdmChangeTypeSupport.CREATE,
                    uom.getVersionNo(),
                    uom.getStatus(),
                    null,
                    uom);
        }
        return saved;
    }

    /**
     * 修改计量单位。
     *
     * @param uom 计量单位对象
     * @return true 表示修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modify(MdmUom uom) {
        if (uom == null || uom.getUomId() == null) {
            return false;
        }
        MdmUom existed = getOne(new LambdaQueryWrapper<MdmUom>()
                .eq(MdmUom::getUomId, uom.getUomId())
                .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        MdmUom before = new MdmUom();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(uom.getUomCode())) {
            String uomCode = normalizeCode(uom.getUomCode());
            if (existsCode(uomCode, uom.getUomId())) {
                return false;
            }
            uom.setUomCode(uomCode);
        }
        if (StringUtils.hasText(uom.getUomName())) {
            uom.setUomName(uom.getUomName().trim());
        }
        if (StringUtils.hasText(uom.getBaseUomCode())) {
            uom.setBaseUomCode(normalizeCode(uom.getBaseUomCode()));
        }
        uom.setStatus(MdmStatusSupport.normalizeStatusForUpdate(uom.getStatus(), existed.getStatus()));
        uom.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        uom.setUpdateBy(resolveOperator());
        uom.setUpdateTime(new Date());
        boolean updated = updateById(uom);
        if (updated) {
            MdmUom after = getById(uom.getUomId());
            auditTrailService.record(MdmDomainTypeSupport.UOM,
                    uom.getUomId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? uom.getVersionNo() : after.getVersionNo(),
                    after == null ? uom.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用计量单位。
     *
     * @param uomId 计量单位ID
     * @return true 表示停用成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long uomId) {
        if (uomId == null) {
            return false;
        }
        MdmUom existed = getOne(new LambdaQueryWrapper<MdmUom>()
                .eq(MdmUom::getUomId, uomId)
                .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmUom updateEntity = new MdmUom();
        updateEntity.setUomId(uomId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmUom after = getById(uomId);
            auditTrailService.record(MdmDomainTypeSupport.UOM,
                    uomId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除计量单位（逻辑删除）。
     *
     * @param uomId 计量单位ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long uomId) {
        if (uomId == null) {
            return false;
        }
        MdmUom existed = getOne(new LambdaQueryWrapper<MdmUom>()
                .eq(MdmUom::getUomId, uomId)
                .eq(MdmUom::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || MdmStatusSupport.isActive(existed.getStatus())) {
            return false;
        }
        MdmUom updateEntity = new MdmUom();
        updateEntity.setUomId(uomId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.UOM,
                    uomId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断编码是否重复。
     *
     * @param code 编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<MdmUom> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmUom::getUomCode, code);
        queryWrapper.eq(MdmUom::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmUom::getUomId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 规范编码文本。
     *
     * @param source 原始文本
     * @return 规范文本
     */
    private String normalizeCode(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        return source.trim().toUpperCase(Locale.ROOT);
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
