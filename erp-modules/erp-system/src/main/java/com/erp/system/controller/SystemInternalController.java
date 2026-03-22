package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import com.erp.platform.contract.model.PlatformImexJob;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;
import com.erp.platform.contract.model.PlatformItemView;
import com.erp.platform.contract.model.PlatformNoticeCreateRequest;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.platform.contract.model.PlatformWarehouseView;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.SysImexJob;
import com.erp.system.domain.SysNotice;
import com.erp.system.domain.SysTenant;
import com.erp.system.domain.SysUser;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmItemService;
import com.erp.system.service.IMdmWarehouseService;
import com.erp.system.service.ISysConfigService;
import com.erp.system.service.ISysImexJobService;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysTenantService;
import com.erp.system.service.ISysUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 平台内部契约控制层。
 */
@RestController
@RequestMapping("/system/internal")
public class SystemInternalController {
    private static final String STATUS_ENABLED = "0";
    private static final String DEL_FLAG_EXISTS = "0";

    private final SecurityUserResolver securityUserResolver;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final ISysConfigService configService;
    private final ISysTenantService tenantService;
    private final ISysImexJobService imexJobService;
    private final IMdmItemService itemService;
    private final IMdmWarehouseService warehouseService;
    private final ISysUserService userService;
    private final ISysNoticeService noticeService;

    public SystemInternalController(SecurityUserResolver securityUserResolver,
            ISysRoleService roleService,
            ISysMenuService menuService,
            ISysConfigService configService,
            ISysTenantService tenantService,
            ISysImexJobService imexJobService,
            IMdmItemService itemService,
            IMdmWarehouseService warehouseService,
            ISysUserService userService,
            ISysNoticeService noticeService) {
        this.securityUserResolver = securityUserResolver;
        this.roleService = roleService;
        this.menuService = menuService;
        this.configService = configService;
        this.tenantService = tenantService;
        this.imexJobService = imexJobService;
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.userService = userService;
        this.noticeService = noticeService;
    }

    /**
     * 查询当前内部主体的权限包。
     *
     * @return 权限包
     */
    @GetMapping("/security/authorities")
    public PlatformAuthorityBundle authorities() {
        Long userId = securityUserResolver.getCurrentUserId();
        PlatformAuthorityBundle bundle = new PlatformAuthorityBundle();
        if (userId == null) {
            bundle.setPermissions(Collections.emptyList());
            bundle.setRoleKeys(Collections.emptyList());
            return bundle;
        }
        bundle.setPermissions(new ArrayList<>(menuService.selectMenuPermsByUserId(userId)));
        bundle.setRoleKeys(new ArrayList<>(roleService.selectRoleKeysByUserId(userId)));
        return bundle;
    }

    /**
     * 查询平台参数值。
     *
     * @param configKey 参数键
     * @return 参数值
     */
    @GetMapping("/config/{configKey}")
    public String configValue(@PathVariable("configKey") String configKey) {
        return configService.selectConfigByKey(configKey);
    }

    /**
     * 查询有效租户列表。
     *
     * @return 有效租户列表
     */
    @GetMapping("/tenants/active")
    public List<PlatformTenantView> activeTenants() {
        List<SysTenant> tenantList = tenantService.list(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getStatus, STATUS_ENABLED)
                .eq(SysTenant::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysTenant::getId));
        List<PlatformTenantView> resultList = new ArrayList<>();
        for (SysTenant tenant : tenantList) {
            resultList.add(toPlatformTenant(tenant));
        }
        return resultList;
    }

    /**
     * 创建平台导入导出任务。
     *
     * @param request 创建参数
     * @return 新建任务
     */
    @PostMapping("/imex/jobs")
    public PlatformImexJob createImexJob(@RequestBody PlatformImexJobCreateRequest request) {
        return toPlatformImexJob(imexJobService.createInternalJob(request));
    }

    /**
     * 查询平台导入导出任务。
     *
     * @param jobId 任务ID
     * @return 任务详情
     */
    @GetMapping("/imex/jobs/{jobId}")
    public PlatformImexJob imexJob(@PathVariable("jobId") Long jobId) {
        return toPlatformImexJob(imexJobService.getDetail(jobId));
    }

    /**
     * 更新平台导入导出任务。
     *
     * @param jobId   任务ID
     * @param request 更新参数
     * @return 更新后的任务
     */
    @PutMapping("/imex/jobs/{jobId}")
    public PlatformImexJob updateImexJob(@PathVariable("jobId") Long jobId,
            @RequestBody PlatformImexJobUpdateRequest request) {
        return toPlatformImexJob(imexJobService.updateInternalJob(jobId, request));
    }

    /**
     * 查询平台物料只读投影。
     *
     * @param itemId 物料ID
     * @return 物料投影
     */
    @GetMapping("/platform/item/{itemId}")
    public PlatformItemView item(@PathVariable("itemId") Long itemId) {
        return toPlatformItem(itemService.getById(itemId));
    }

    /**
     * 查询平台仓库只读投影。
     *
     * @param warehouseId 仓库ID
     * @return 仓库投影
     */
    @GetMapping("/platform/warehouse/{warehouseId}")
    public PlatformWarehouseView warehouse(@PathVariable("warehouseId") Long warehouseId) {
        return toPlatformWarehouse(warehouseService.getById(warehouseId));
    }

    /**
     * 按账号查询活动用户。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 用户投影
     */
    @GetMapping("/platform/user/by-username")
    public PlatformUserView activeUserByUsername(@RequestParam("tenantId") String tenantId,
            @RequestParam("userName") String userName) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userName)) {
            return null;
        }
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId.trim())
                .eq(SysUser::getUserName, userName.trim())
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXISTS)
                .last("LIMIT 1"));
        return toPlatformUser(user);
    }

    /**
     * 查询首个活动用户。
     *
     * @param tenantId 租户编号
     * @return 用户投影
     */
    @GetMapping("/platform/user/first-active")
    public PlatformUserView firstActiveUser(@RequestParam("tenantId") String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId.trim())
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysUser::getUserId)
                .last("LIMIT 1"));
        return toPlatformUser(user);
    }

    /**
     * 创建平台站内通知。
     *
     * @param request 通知参数
     * @return 通知ID
     */
    @PostMapping("/platform/notices")
    public Long createNotice(@RequestBody PlatformNoticeCreateRequest request) {
        if (request == null) {
            return null;
        }
        SysNotice notice = new SysNotice();
        notice.setTenantId(trimToNull(request.getTenantId()));
        notice.setTitle(trimToNull(request.getTitle()));
        notice.setNoticeType(trimToNull(request.getNoticeType()));
        notice.setSource(trimToNull(request.getSource()));
        notice.setBusinessNo(trimToNull(request.getBusinessNo()));
        notice.setContent(trimToNull(request.getContent()));
        notice.setReceiverUserId(request.getReceiverUserId());
        notice.setDeliveryChannel(trimToNull(request.getDeliveryChannel()));
        notice.setDeliveryStatus(trimToNull(request.getDeliveryStatus()));
        notice.setDeliveryTime(request.getDeliveryTime());
        notice.setStatus(trimToNull(request.getStatus()));
        notice.setCreateTime(request.getCreateTime());
        noticeService.createNotice(notice);
        return notice.getNoticeId();
    }

    /**
     * 转换平台租户投影。
     *
     * @param tenant 租户对象
     * @return 平台租户投影
     */
    private PlatformTenantView toPlatformTenant(SysTenant tenant) {
        if (tenant == null) {
            return null;
        }
        PlatformTenantView view = new PlatformTenantView();
        view.setId(tenant.getId());
        view.setTenantId(tenant.getTenantId());
        view.setTenantName(tenant.getName());
        view.setStatus(tenant.getStatus());
        view.setDelFlag(tenant.getDelFlag());
        return view;
    }

    /**
     * 转换平台导入导出任务投影。
     *
     * @param job 任务对象
     * @return 平台任务投影
     */
    private PlatformImexJob toPlatformImexJob(SysImexJob job) {
        if (job == null) {
            return null;
        }
        PlatformImexJob view = new PlatformImexJob();
        view.setJobId(job.getJobId());
        view.setTenantId(job.getTenantId());
        view.setJobNo(job.getJobNo());
        view.setJobType(job.getJobType());
        view.setModuleCode(job.getModuleCode());
        view.setFileName(job.getFileName());
        view.setFilePath(job.getFilePath());
        view.setStatus(job.getStatus());
        view.setProgress(job.getProgress());
        view.setTriggerType(job.getTriggerType());
        view.setMessage(job.getMessage());
        view.setCreateBy(job.getCreateBy());
        view.setCreateTime(job.getCreateTime());
        view.setUpdateBy(job.getUpdateBy());
        view.setUpdateTime(job.getUpdateTime());
        return view;
    }

    /**
     * 转换平台物料投影。
     *
     * @param item 物料对象
     * @return 物料投影
     */
    private PlatformItemView toPlatformItem(MdmItem item) {
        if (item == null) {
            return null;
        }
        PlatformItemView view = new PlatformItemView();
        view.setItemId(item.getItemId());
        view.setTenantId(item.getTenantId());
        view.setItemCode(item.getItemCode());
        view.setItemName(item.getItemName());
        view.setShelfLifeDays(item.getShelfLifeDays());
        view.setDefaultExpiryWarnDays(item.getDefaultExpiryWarnDays());
        view.setBatchControl(item.getBatchControl());
        view.setSerialControl(item.getSerialControl());
        view.setStatus(item.getStatus());
        return view;
    }

    /**
     * 转换平台仓库投影。
     *
     * @param warehouse 仓库对象
     * @return 仓库投影
     */
    private PlatformWarehouseView toPlatformWarehouse(MdmWarehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        PlatformWarehouseView view = new PlatformWarehouseView();
        view.setWarehouseId(warehouse.getWarehouseId());
        view.setAccountingOrgId(warehouse.getAccountingOrgId());
        view.setAllowNegativeStock(warehouse.getAllowNegativeStock());
        return view;
    }

    /**
     * 转换平台用户投影。
     *
     * @param user 用户对象
     * @return 用户投影
     */
    private PlatformUserView toPlatformUser(SysUser user) {
        if (user == null) {
            return null;
        }
        PlatformUserView view = new PlatformUserView();
        view.setUserId(user.getUserId());
        view.setTenantId(user.getTenantId());
        view.setUserName(user.getUserName());
        view.setNickName(user.getNickName());
        view.setStatus(user.getStatus());
        view.setDelFlag(user.getDelFlag());
        return view;
    }

    /**
     * 规范化文本值。
     *
     * @param value 原始文本
     * @return 规范化结果
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

