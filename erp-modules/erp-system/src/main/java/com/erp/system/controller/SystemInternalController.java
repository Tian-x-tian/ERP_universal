package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.platform.contract.model.PlatformAuthorityBundle;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.platform.contract.model.PlatformDeptView;
import com.erp.platform.contract.model.PlatformImexJob;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;
import com.erp.platform.contract.model.PlatformItemView;
import com.erp.platform.contract.model.PlatformNoticeCreateRequest;
import com.erp.platform.contract.model.PlatformOperationLogCreateRequest;
import com.erp.platform.contract.model.PlatformPostView;
import com.erp.platform.contract.model.PlatformRoleView;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.platform.contract.model.PlatformUserPostLink;
import com.erp.platform.contract.model.PlatformUserRoleLink;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.platform.contract.model.PlatformWarehouseView;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysImexJob;
import com.erp.system.domain.SysNotice;
import com.erp.system.domain.SysPost;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysTenant;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserPost;
import com.erp.system.domain.SysUserRole;
import com.erp.common.logging.OperationLogPayload;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmItemService;
import com.erp.system.service.IMdmWarehouseService;
import com.erp.system.service.ISysAiAuditService;
import com.erp.system.service.ISysAiConfigService;
import com.erp.system.service.ISysConfigService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysImexJobService;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysPostService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysTenantService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysUserPostService;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.saas.SaasLocalQuotaService;
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
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ISysAiConfigService aiConfigService;
    private final ISysAiAuditService aiAuditService;
    private final OperationLogRecorder operationLogRecorder;
    private final IMdmItemService itemService;
    private final IMdmWarehouseService warehouseService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysPostService postService;
    private final ISysUserRoleService userRoleService;
    private final ISysUserPostService userPostService;
    private final ISysNoticeService noticeService;
    private final SaasLocalQuotaService quotaService;

    public SystemInternalController(SecurityUserResolver securityUserResolver,
            ISysRoleService roleService,
            ISysMenuService menuService,
            ISysConfigService configService,
            ISysTenantService tenantService,
            ISysImexJobService imexJobService,
            ISysAiConfigService aiConfigService,
            ISysAiAuditService aiAuditService,
            IMdmItemService itemService,
            IMdmWarehouseService warehouseService,
            ISysUserService userService,
            ISysDeptService deptService,
            ISysPostService postService,
            ISysUserRoleService userRoleService,
            ISysUserPostService userPostService,
            ISysNoticeService noticeService,
            OperationLogRecorder operationLogRecorder,
            SaasLocalQuotaService quotaService) {
        this.operationLogRecorder = operationLogRecorder;
        this.securityUserResolver = securityUserResolver;
        this.roleService = roleService;
        this.menuService = menuService;
        this.configService = configService;
        this.tenantService = tenantService;
        this.imexJobService = imexJobService;
        this.aiConfigService = aiConfigService;
        this.aiAuditService = aiAuditService;
        this.itemService = itemService;
        this.warehouseService = warehouseService;
        this.userService = userService;
        this.deptService = deptService;
        this.postService = postService;
        this.userRoleService = userRoleService;
        this.userPostService = userPostService;
        this.noticeService = noticeService;
        this.quotaService = quotaService;
    }

    /**
     * Applies a tenant-local quota event for trusted internal callers.
     *
     * @param event quota event bound to the signed tenant context
     * @return current local usage
     */
    @PostMapping("/saas/quotas/events")
    public SaasQuotaUsage applyQuotaEvent(@RequestBody SaasUsageEvent event) {
        return quotaService.apply(event);
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
     * 查询活动用户列表，支持按用户ID集合筛选。
     *
     * @param ids 用户ID集合
     * @return 用户列表
     */
    @GetMapping("/platform/users")
    public List<PlatformUserView> users(@RequestParam(value = "ids", required = false) String ids) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysUser::getUserId);
        Set<Long> userIdSet = parseIds(ids);
        if (!userIdSet.isEmpty()) {
            queryWrapper.in(SysUser::getUserId, userIdSet);
        }
        return userService.list(queryWrapper).stream()
                .map(this::toPlatformUser)
                .collect(Collectors.toList());
    }

    /**
     * 按部门查询活动用户列表。
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    @GetMapping("/platform/users/by-dept")
    public List<PlatformUserView> usersByDept(@RequestParam("deptId") Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return userService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysUser::getUserId))
                .stream()
                .map(this::toPlatformUser)
                .collect(Collectors.toList());
    }

    /**
     * 查询单个活动用户。
     *
     * @param userId 用户ID
     * @return 用户投影
     */
    @GetMapping("/platform/users/{userId}")
    public PlatformUserView user(@PathVariable("userId") Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXISTS)
                .last("LIMIT 1"));
        return toPlatformUser(user);
    }

    /**
     * 查询活动部门列表，支持按部门ID集合筛选。
     *
     * @param ids 部门ID集合
     * @return 部门列表
     */
    @GetMapping("/platform/departments")
    public List<PlatformDeptView> departments(@RequestParam(value = "ids", required = false) String ids) {
        LambdaQueryWrapper<SysDept> queryWrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, STATUS_ENABLED)
                .eq(SysDept::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysDept::getDeptId);
        Set<Long> deptIdSet = parseIds(ids);
        if (!deptIdSet.isEmpty()) {
            queryWrapper.in(SysDept::getDeptId, deptIdSet);
        }
        return deptService.list(queryWrapper).stream()
                .map(this::toPlatformDept)
                .collect(Collectors.toList());
    }

    /**
     * 查询单个活动部门。
     *
     * @param deptId 部门ID
     * @return 部门投影
     */
    @GetMapping("/platform/departments/{deptId}")
    public PlatformDeptView department(@PathVariable("deptId") Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = deptService.getOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptId, deptId)
                .eq(SysDept::getStatus, STATUS_ENABLED)
                .eq(SysDept::getDelFlag, DEL_FLAG_EXISTS)
                .last("LIMIT 1"));
        return toPlatformDept(dept);
    }

    /**
     * 查询活动角色列表。
     *
     * @return 角色列表
     */
    @GetMapping("/platform/roles")
    public List<PlatformRoleView> roles() {
        return roleService.list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, STATUS_ENABLED)
                .eq(SysRole::getDelFlag, DEL_FLAG_EXISTS)
                .orderByAsc(SysRole::getRoleSort)
                .orderByAsc(SysRole::getRoleId))
                .stream()
                .map(this::toPlatformRole)
                .collect(Collectors.toList());
    }

    /**
     * 查询活动岗位列表。
     *
     * @return 岗位列表
     */
    @GetMapping("/platform/posts")
    public List<PlatformPostView> posts() {
        return postService.list(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, STATUS_ENABLED)
                .orderByAsc(SysPost::getPostSort)
                .orderByAsc(SysPost::getPostId))
                .stream()
                .map(this::toPlatformPost)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户角色关联列表。
     *
     * @param roleIds 角色ID集合
     * @return 关联列表
     */
    @GetMapping("/platform/user-role-links")
    public List<PlatformUserRoleLink> userRoleLinks(@RequestParam(value = "roleIds", required = false) String roleIds) {
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        Set<Long> roleIdSet = parseIds(roleIds);
        if (!roleIdSet.isEmpty()) {
            queryWrapper.in(SysUserRole::getRoleId, roleIdSet);
        }
        return userRoleService.list(queryWrapper).stream()
                .map(this::toPlatformUserRoleLink)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户岗位关联列表。
     *
     * @param postIds 岗位ID集合
     * @return 关联列表
     */
    @GetMapping("/platform/user-post-links")
    public List<PlatformUserPostLink> userPostLinks(@RequestParam(value = "postIds", required = false) String postIds) {
        LambdaQueryWrapper<SysUserPost> queryWrapper = new LambdaQueryWrapper<>();
        Set<Long> postIdSet = parseIds(postIds);
        if (!postIdSet.isEmpty()) {
            queryWrapper.in(SysUserPost::getPostId, postIdSet);
        }
        return userPostService.list(queryWrapper).stream()
                .map(this::toPlatformUserPostLink)
                .collect(Collectors.toList());
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

    @GetMapping("/platform/notices/{noticeId}")
    public com.erp.platform.contract.model.PlatformNoticeView getNotice(@PathVariable("noticeId") Long noticeId) {
        SysNotice notice = noticeService.getById(noticeId);
        if (notice == null)
            return null;
        com.erp.platform.contract.model.PlatformNoticeView view = new com.erp.platform.contract.model.PlatformNoticeView();
        view.setNoticeId(notice.getNoticeId());
        view.setReceiverUserId(notice.getReceiverUserId());
        view.setTitle(notice.getTitle());
        view.setBusinessNo(notice.getBusinessNo());
        view.setSource(notice.getSource());
        view.setNoticeType(notice.getNoticeType());
        view.setStatus(notice.getStatus());
        view.setDeliveryStatus(notice.getDeliveryStatus());
        view.setCreateTime(notice.getCreateTime());
        return view;
    }

    @GetMapping("/platform/notices/unread-count")
    public Long countUnreadNotices(@RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId != null ? userId : securityUserResolver.getCurrentUserId();
        return noticeService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getReceiverUserId, targetUserId)
                .eq(SysNotice::getStatus, "0"));
    }

    @GetMapping("/platform/notices/latest")
    public List<com.erp.platform.contract.model.PlatformNoticeView> getLatestNotices(
            @RequestParam(value = "userId", required = false) Long userId, @RequestParam("limit") int limit) {
        Long targetUserId = userId != null ? userId : securityUserResolver.getCurrentUserId();
        List<SysNotice> list = noticeService
                .list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getReceiverUserId, targetUserId)
                        .orderByDesc(SysNotice::getCreateTime)
                        .last("LIMIT " + limit));
        if (list == null)
            return Collections.emptyList();
        List<com.erp.platform.contract.model.PlatformNoticeView> views = new ArrayList<>();
        for (SysNotice notice : list) {
            com.erp.platform.contract.model.PlatformNoticeView view = new com.erp.platform.contract.model.PlatformNoticeView();
            view.setNoticeId(notice.getNoticeId());
            view.setReceiverUserId(notice.getReceiverUserId());
            view.setTitle(notice.getTitle());
            view.setBusinessNo(notice.getBusinessNo());
            view.setSource(notice.getSource());
            view.setNoticeType(notice.getNoticeType());
            view.setStatus(notice.getStatus());
            view.setDeliveryStatus(notice.getDeliveryStatus());
            view.setCreateTime(notice.getCreateTime());
            views.add(view);
        }
        return views;
    }

    @PostMapping("/platform/notices/{noticeId}/read")
    public Boolean markNoticeRead(@PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId != null ? userId : securityUserResolver.getCurrentUserId();
        return noticeService.markRead(noticeId, targetUserId);
    }

    @PostMapping("/platform/notices/read-all")
    public Integer markAllNoticeRead(@RequestParam(value = "userId", required = false) Long userId) {
        Long targetUserId = userId != null ? userId : securityUserResolver.getCurrentUserId();
        return noticeService.markAllRead(targetUserId);
    }

    @GetMapping("/platform/permissions/check")
    public Boolean hasPermission(@RequestParam("permission") String permission) {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null || !StringUtils.hasText(permission)) {
            return false;
        }
        if (roleService.isPlatformSuperAdmin(userId)) {
            return true;
        }
        Set<String> perms = menuService.selectMenuPermsByUserId(userId);
        if (perms == null) {
            return false;
        }
        for (String p : perms) {
            if ("*:*:*".equals(p) || permission.equals(p)) {
                return true;
            }
            if (p.endsWith(":*") && permission.startsWith(p.substring(0, p.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询当前租户 AI 配置。
     *
     * @return AI 配置
     */
    @GetMapping("/platform/ai/config")
    public PlatformAiConfigView aiConfig() {
        return aiConfigService.getTenantConfig(securityUserResolver.getCurrentTenantId());
    }

    /**
     * 更新当前租户 AI 配置。
     *
     * @param request 更新请求
     * @return 更新后的 AI 配置
     */
    @PutMapping("/platform/ai/config")
    public PlatformAiConfigView updateAiConfig(@RequestBody(required = false) PlatformAiConfigUpdateRequest request) {
        return aiConfigService.updateTenantConfig(securityUserResolver.getCurrentTenantId(), request);
    }

    /**
     * 查询当前租户 AI 动作策略。
     *
     * @return 动作策略列表
     */
    @GetMapping("/platform/ai/policy/actions")
    public List<PlatformAiActionPolicyItem> aiActionPolicies() {
        return aiConfigService.listActionPolicies(securityUserResolver.getCurrentTenantId());
    }

    /**
     * 更新当前租户 AI 动作策略。
     *
     * @param policyItems 动作策略列表
     * @return 更新后的动作策略列表
     */
    @PutMapping("/platform/ai/policy/actions")
    public List<PlatformAiActionPolicyItem> updateAiActionPolicies(
            @RequestBody(required = false) List<PlatformAiActionPolicyItem> policyItems) {
        return aiConfigService.updateActionPolicies(securityUserResolver.getCurrentTenantId(), policyItems);
    }

    /**
     * 接收其他服务回传的操作/审计日志并落库。
     * 非 erp-system 的服务不直接写 sys_oper_log / sys_audit_log，统一走本接口。
     *
     * @param request 日志写入请求
     */
    @PostMapping("/platform/oper-log")
    public void recordOperationLog(@RequestBody(required = false) PlatformOperationLogCreateRequest request) {
        if (request == null) {
            return;
        }
        OperationLogPayload payload = new OperationLogPayload();
        payload.setLogType(StringUtils.hasText(request.getLogType())
                ? request.getLogType()
                : OperationLogPayload.TYPE_OPERATION);
        payload.setTenantId(request.getTenantId());
        payload.setOperator(request.getOperator());
        payload.setOperationType(request.getOperationType());
        payload.setRequestMethod(request.getRequestMethod());
        payload.setRequestUri(request.getRequestUri());
        payload.setRequestIp(request.getRequestIp());
        payload.setRequestParams(request.getRequestParams());
        payload.setResponseCode(request.getResponseCode());
        payload.setSuccessFlag(request.getSuccessFlag());
        payload.setErrorMsg(request.getErrorMsg());
        payload.setCostTime(request.getCostTime());
        payload.setOperationTime(request.getOperationTime() == null ? new Date() : request.getOperationTime());
        operationLogRecorder.record(payload);
    }

    /**
     * 写入 AI 审计记录。
     *
     * @param request 审计写入请求
     */
    @PostMapping("/platform/ai/audit")
    public void recordAiAudit(@RequestBody(required = false) PlatformAiAuditCreateRequest request) {
        aiAuditService.record(
                securityUserResolver.getCurrentTenantId(),
                securityUserResolver.getCurrentUserId(),
                securityUserResolver.getCurrentUsername(),
                request);
    }

    /**
     * 查询当前租户 AI 审计记录。
     *
     * @param limit 查询条数
     * @return 审计记录列表
     */
    @GetMapping("/platform/ai/audit")
    public List<PlatformAiAuditView> aiAuditList(@RequestParam(value = "limit", required = false) Integer limit) {
        return aiAuditService.listByTenant(
                securityUserResolver.getCurrentTenantId(),
                limit == null ? 50 : limit);
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
     * 转换平台部门投影。
     *
     * @param dept 部门对象
     * @return 部门投影
     */
    private PlatformDeptView toPlatformDept(SysDept dept) {
        if (dept == null) {
            return null;
        }
        PlatformDeptView view = new PlatformDeptView();
        view.setDeptId(dept.getDeptId());
        view.setTenantId(dept.getTenantId());
        view.setParentId(dept.getParentId());
        view.setDeptName(dept.getDeptName());
        view.setLeader(dept.getLeader());
        view.setStatus(dept.getStatus());
        view.setDelFlag(dept.getDelFlag());
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
        view.setDeptId(user.getDeptId());
        view.setUserName(user.getUserName());
        view.setNickName(user.getNickName());
        view.setStatus(user.getStatus());
        view.setDelFlag(user.getDelFlag());
        return view;
    }

    /**
     * 转换平台角色投影。
     *
     * @param role 角色对象
     * @return 角色投影
     */
    private PlatformRoleView toPlatformRole(SysRole role) {
        if (role == null) {
            return null;
        }
        PlatformRoleView view = new PlatformRoleView();
        view.setRoleId(role.getRoleId());
        view.setTenantId(role.getTenantId());
        view.setRoleName(role.getRoleName());
        view.setRoleKey(role.getRoleKey());
        view.setStatus(role.getStatus());
        view.setDelFlag(role.getDelFlag());
        return view;
    }

    /**
     * 转换平台岗位投影。
     *
     * @param post 岗位对象
     * @return 岗位投影
     */
    private PlatformPostView toPlatformPost(SysPost post) {
        if (post == null) {
            return null;
        }
        PlatformPostView view = new PlatformPostView();
        view.setPostId(post.getPostId());
        view.setTenantId(post.getTenantId());
        view.setPostCode(post.getPostCode());
        view.setPostName(post.getPostName());
        view.setStatus(post.getStatus());
        return view;
    }

    /**
     * 转换平台用户角色关联投影。
     *
     * @param relation 关联对象
     * @return 关联投影
     */
    private PlatformUserRoleLink toPlatformUserRoleLink(SysUserRole relation) {
        if (relation == null) {
            return null;
        }
        PlatformUserRoleLink view = new PlatformUserRoleLink();
        view.setTenantId(relation.getTenantId());
        view.setUserId(relation.getUserId());
        view.setRoleId(relation.getRoleId());
        return view;
    }

    /**
     * 转换平台用户岗位关联投影。
     *
     * @param relation 关联对象
     * @return 关联投影
     */
    private PlatformUserPostLink toPlatformUserPostLink(SysUserPost relation) {
        if (relation == null) {
            return null;
        }
        PlatformUserPostLink view = new PlatformUserPostLink();
        view.setTenantId(relation.getTenantId());
        view.setUserId(relation.getUserId());
        view.setPostId(relation.getPostId());
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

    /**
     * 将逗号分隔字符串解析为 ID 集合。
     *
     * @param ids 原始文本
     * @return ID 集合
     */
    private Set<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String token : ids.split(",")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            try {
                result.add(Long.valueOf(token.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略非法参数，保持内部接口幂等和容错。
            }
        }
        return result;
    }
}
