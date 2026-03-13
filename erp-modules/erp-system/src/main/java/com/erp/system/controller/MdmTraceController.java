package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
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
     * @param bizId      业务主键ID
     * @param pageNum    当前页码
     * @param pageSize   每页条数
     * @return 变更日志列表
     */
    @GetMapping("/log/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:list')")
    public R<PageData<MdmChangeLog>> logList(@RequestParam(value = "domainType", required = false) String domainType,
            @RequestParam(value = "bizId", required = false) Long bizId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        LambdaQueryWrapper<MdmChangeLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(domainType)) {
            queryWrapper.eq(MdmChangeLog::getDomainType, domainType.trim().toUpperCase(Locale.ROOT));
        }
        if (bizId != null) {
            queryWrapper.eq(MdmChangeLog::getBizId, bizId);
        }
        queryWrapper.orderByDesc(MdmChangeLog::getCreateTime).orderByDesc(MdmChangeLog::getLogId);
        Page<MdmChangeLog> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<MdmChangeLog> resultPage = changeLogService.page(page, queryWrapper);
        return R.page(resultPage.getRecords(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    /**
     * 根据域类型与业务ID查询变更日志。
     *
     * @param domainType 主数据域类型
     * @param bizId      业务主键ID
     * @param pageNum    当前页码
     * @param pageSize   每页条数
     * @return 变更日志列表
     */
    @GetMapping("/log/{domainType}/{bizId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:query')")
    public R<PageData<MdmChangeLog>> logByBiz(@PathVariable("domainType") String domainType,
            @PathVariable("bizId") Long bizId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return logList(domainType, bizId, pageNum, pageSize);
    }

    /**
     * 查询版本快照列表。
     *
     * @param domainType 主数据域类型
     * @param bizId      业务主键ID
     * @param pageNum    当前页码
     * @param pageSize   每页条数
     * @return 版本快照列表
     */
    @GetMapping("/version/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:trace:list')")
    public R<PageData<MdmVersion>> versionList(@RequestParam(value = "domainType", required = false) String domainType,
            @RequestParam(value = "bizId", required = false) Long bizId,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        LambdaQueryWrapper<MdmVersion> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(domainType)) {
            queryWrapper.eq(MdmVersion::getDomainType, domainType.trim().toUpperCase(Locale.ROOT));
        }
        if (bizId != null) {
            queryWrapper.eq(MdmVersion::getBizId, bizId);
        }
        queryWrapper.orderByDesc(MdmVersion::getVersionNo).orderByDesc(MdmVersion::getVersionId);
        Page<MdmVersion> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<MdmVersion> resultPage = versionService.page(page, queryWrapper);
        return R.page(resultPage.getRecords(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    /**
     * 规范化页码，避免非法页码导致分页异常。
     *
     * @param pageNum 原始页码
     * @return 规范化后的页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化分页大小，统一限制最大页长。
     *
     * @param pageSize 原始分页大小
     * @return 规范化后的分页大小
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
