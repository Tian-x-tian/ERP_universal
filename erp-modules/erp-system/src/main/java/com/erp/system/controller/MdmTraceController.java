package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmChangeLog;
import com.erp.system.domain.MdmVersion;
import com.erp.system.service.IMdmChangeLogService;
import com.erp.system.service.IMdmVersionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * MDM 变更追踪控制层。
 */
@RestController
@RequestMapping("/system/mdm/trace")
public class MdmTraceController {
    private final IMdmChangeLogService changeLogService;
    private final IMdmVersionService versionService;

    public MdmTraceController(IMdmChangeLogService changeLogService, IMdmVersionService versionService) {
        this.changeLogService = changeLogService;
        this.versionService = versionService;
    }

    /**
     * 查询主数据变更时间线。
     *
     * @param domainType 主数据域类型
     * @param bizId 业务主键ID
     * @return 变更日志列表
     */
    @GetMapping("/log/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:list')")
    public R<List<MdmChangeLog>> logList(@RequestParam(value = "domainType", required = false) String domainType,
            @RequestParam(value = "bizId", required = false) Long bizId) {
        LambdaQueryWrapper<MdmChangeLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(domainType)) {
            queryWrapper.eq(MdmChangeLog::getDomainType, domainType.trim().toUpperCase(Locale.ROOT));
        }
        if (bizId != null) {
            queryWrapper.eq(MdmChangeLog::getBizId, bizId);
        }
        queryWrapper.orderByDesc(MdmChangeLog::getCreateTime).orderByDesc(MdmChangeLog::getLogId);
        return R.success(changeLogService.list(queryWrapper));
    }

    /**
     * 根据域类型与业务ID查询变更日志。
     *
     * @param domainType 主数据域类型
     * @param bizId 业务主键ID
     * @return 变更日志列表
     */
    @GetMapping("/log/{domainType}/{bizId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:query')")
    public R<List<MdmChangeLog>> logByBiz(@PathVariable("domainType") String domainType,
            @PathVariable("bizId") Long bizId) {
        return logList(domainType, bizId);
    }

    /**
     * 查询版本快照列表。
     *
     * @param domainType 主数据域类型
     * @param bizId 业务主键ID
     * @return 版本快照列表
     */
    @GetMapping("/version/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:list')")
    public R<List<MdmVersion>> versionList(@RequestParam(value = "domainType", required = false) String domainType,
            @RequestParam(value = "bizId", required = false) Long bizId) {
        LambdaQueryWrapper<MdmVersion> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(domainType)) {
            queryWrapper.eq(MdmVersion::getDomainType, domainType.trim().toUpperCase(Locale.ROOT));
        }
        if (bizId != null) {
            queryWrapper.eq(MdmVersion::getBizId, bizId);
        }
        queryWrapper.orderByDesc(MdmVersion::getVersionNo).orderByDesc(MdmVersion::getVersionId);
        return R.success(versionService.list(queryWrapper));
    }
}
