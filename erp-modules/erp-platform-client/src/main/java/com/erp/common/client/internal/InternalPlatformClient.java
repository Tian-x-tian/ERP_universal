package com.erp.common.client.internal;

import com.erp.platform.contract.model.PlatformAuthorityBundle;
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
import com.erp.workflow.contract.domain.vo.WorkflowCallbackEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 平台内部接口客户端。
 */
@Component
public class InternalPlatformClient {
    private final RestTemplate restTemplate;
    private final InternalRequestHeaderFactory headerFactory;
    private final InternalSystemClientProperties properties;

    public InternalPlatformClient(RestTemplate internalSystemRestTemplate,
            InternalRequestHeaderFactory headerFactory,
            InternalSystemClientProperties properties) {
        this.restTemplate = internalSystemRestTemplate;
        this.headerFactory = headerFactory;
        this.properties = properties;
    }

    /**
     * 查询当前内部主体的权限包。
     *
     * @return 权限包
     */
    public PlatformAuthorityBundle getAuthorities() {
        return exchange(buildUri("/system/internal/security/authorities"),
                HttpMethod.GET,
                null,
                PlatformAuthorityBundle.class);
    }

    /**
     * 查询平台参数值。
     *
     * @param configKey 参数键
     * @return 参数值
     */
    public String getConfigValue(String configKey) {
        return exchange(buildUri("/system/internal/config/" + configKey),
                HttpMethod.GET,
                null,
                String.class);
    }

    /**
     * 查询有效租户列表。
     *
     * @return 有效租户列表
     */
    public List<PlatformTenantView> listActiveTenants() {
        ResponseEntity<List<PlatformTenantView>> response = restTemplate.exchange(
                buildUri("/system/internal/tenants/active"),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<PlatformTenantView>>() {
                });
        List<PlatformTenantView> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 创建平台导入导出任务。
     *
     * @param request 创建参数
     * @return 任务结果
     */
    public PlatformImexJob createImexJob(PlatformImexJobCreateRequest request) {
        return exchange(buildUri("/system/internal/imex/jobs"),
                HttpMethod.POST,
                request,
                PlatformImexJob.class);
    }

    /**
     * 查询平台导入导出任务详情。
     *
     * @param jobId 任务ID
     * @return 任务结果
     */
    public PlatformImexJob getImexJob(Long jobId) {
        return exchange(buildUri("/system/internal/imex/jobs/" + jobId),
                HttpMethod.GET,
                null,
                PlatformImexJob.class);
    }

    /**
     * 更新平台导入导出任务。
     *
     * @param jobId   任务ID
     * @param request 更新参数
     * @return 更新后的任务
     */
    public PlatformImexJob updateImexJob(Long jobId, PlatformImexJobUpdateRequest request) {
        return exchange(buildUri("/system/internal/imex/jobs/" + jobId),
                HttpMethod.PUT,
                request,
                PlatformImexJob.class);
    }

    /**
     * 查询平台物料只读投影。
     *
     * @param itemId 物料ID
     * @return 物料投影
     */
    public PlatformItemView getItem(Long itemId) {
        return exchange(buildUri("/system/internal/platform/item/" + itemId),
                HttpMethod.GET,
                null,
                PlatformItemView.class);
    }

    /**
     * 查询平台仓库只读投影。
     *
     * @param warehouseId 仓库ID
     * @return 仓库投影
     */
    public PlatformWarehouseView getWarehouse(Long warehouseId) {
        return exchange(buildUri("/system/internal/platform/warehouse/" + warehouseId),
                HttpMethod.GET,
                null,
                PlatformWarehouseView.class);
    }

    /**
     * 按账号查询活动用户。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 用户投影
     */
    public PlatformUserView getActiveUserByUsername(String tenantId, String userName) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/user/by-username"))
                .queryParam("tenantId", tenantId)
                .queryParam("userName", userName)
                .build(true)
                .toUri();
        return exchange(uri, HttpMethod.GET, null, PlatformUserView.class);
    }

    /**
     * 查询首个活动用户。
     *
     * @param tenantId 租户编号
     * @return 用户投影
     */
    public PlatformUserView getFirstActiveUser(String tenantId) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/user/first-active"))
                .queryParam("tenantId", tenantId)
                .build(true)
                .toUri();
        return exchange(uri, HttpMethod.GET, null, PlatformUserView.class);
    }

    /**
     * 查询活动用户列表，支持按用户ID集合筛选。
     *
     * @param userIds 用户ID集合
     * @return 用户列表
     */
    public List<PlatformUserView> listUsers(Collection<Long> userIds) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/users"))
                .queryParamIfPresent("ids", joinIds(userIds))
                .build(true)
                .toUri();
        return exchangeList(uri, new ParameterizedTypeReference<List<PlatformUserView>>() {
        });
    }

    /**
     * 查询指定部门下的活动用户。
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    public List<PlatformUserView> listUsersByDeptId(Long deptId) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/users/by-dept"))
                .queryParam("deptId", deptId)
                .build(true)
                .toUri();
        return exchangeList(uri, new ParameterizedTypeReference<List<PlatformUserView>>() {
        });
    }

    /**
     * 查询单个用户只读投影。
     *
     * @param userId 用户ID
     * @return 用户投影
     */
    public PlatformUserView getUser(Long userId) {
        return exchange(buildUri("/system/internal/platform/users/" + userId),
                HttpMethod.GET,
                null,
                PlatformUserView.class);
    }

    /**
     * 查询部门列表，支持按部门ID集合筛选。
     *
     * @param deptIds 部门ID集合
     * @return 部门列表
     */
    public List<PlatformDeptView> listDepartments(Collection<Long> deptIds) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/departments"))
                .queryParamIfPresent("ids", joinIds(deptIds))
                .build(true)
                .toUri();
        return exchangeList(uri, new ParameterizedTypeReference<List<PlatformDeptView>>() {
        });
    }

    /**
     * 查询单个部门只读投影。
     *
     * @param deptId 部门ID
     * @return 部门投影
     */
    public PlatformDeptView getDepartment(Long deptId) {
        return exchange(buildUri("/system/internal/platform/departments/" + deptId),
                HttpMethod.GET,
                null,
                PlatformDeptView.class);
    }

    /**
     * 查询活动角色列表。
     *
     * @return 角色列表
     */
    public List<PlatformRoleView> listRoles() {
        return exchangeList(buildUri("/system/internal/platform/roles"),
                new ParameterizedTypeReference<List<PlatformRoleView>>() {
                });
    }

    /**
     * 查询活动岗位列表。
     *
     * @return 岗位列表
     */
    public List<PlatformPostView> listPosts() {
        return exchangeList(buildUri("/system/internal/platform/posts"),
                new ParameterizedTypeReference<List<PlatformPostView>>() {
                });
    }

    /**
     * 查询用户角色关联。
     *
     * @param roleIds 角色ID集合
     * @return 关联列表
     */
    public List<PlatformUserRoleLink> listUserRoleLinks(Collection<Long> roleIds) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/user-role-links"))
                .queryParamIfPresent("roleIds", joinIds(roleIds))
                .build(true)
                .toUri();
        return exchangeList(uri, new ParameterizedTypeReference<List<PlatformUserRoleLink>>() {
        });
    }

    /**
     * 查询用户岗位关联。
     *
     * @param postIds 岗位ID集合
     * @return 关联列表
     */
    public List<PlatformUserPostLink> listUserPostLinks(Collection<Long> postIds) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/user-post-links"))
                .queryParamIfPresent("postIds", joinIds(postIds))
                .build(true)
                .toUri();
        return exchangeList(uri, new ParameterizedTypeReference<List<PlatformUserPostLink>>() {
        });
    }

    /**
     * 创建平台站内通知。
     *
     * @param request 通知参数
     * @return 通知ID
     */
    public Long createNotice(PlatformNoticeCreateRequest request) {
        return exchange(buildUri("/system/internal/platform/notices"),
                HttpMethod.POST,
                request,
                Long.class);
    }

    /**
     * 推送工作流终态回调事件。
     *
     * @param event 回调事件
     */
    public void notifyWorkflowCallback(WorkflowCallbackEvent event) {
        exchange(buildUri("/system/internal/workflow/callbacks/terminal"),
                HttpMethod.POST,
                event,
                Void.class);
    }

    /**
     * 回传操作/审计日志给 erp-system 落库。
     *
     * @param request 日志写入请求
     */
    public void recordOperationLog(PlatformOperationLogCreateRequest request) {
        exchange(buildUri("/system/internal/platform/oper-log"),
                HttpMethod.POST,
                request,
                Void.class);
    }

    /**
     * 发起内部 HTTP 调用。
     *
     * @param uri          目标地址
     * @param method       请求方法
     * @param body         请求体
     * @param responseType 响应类型
     * @param <T>          响应泛型
     * @return 响应对象
     */
    private <T> T exchange(URI uri, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = headerFactory.buildHeaders();
        ResponseEntity<T> response = restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), responseType);
        return response.getBody();
    }

    /**
     * 发起返回集合的内部 HTTP 调用。
     *
     * @param uri 目标地址
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应集合
     */
    private <T> T exchangeList(URI uri, ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = headerFactory.buildHeaders();
        ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), responseType);
        return response.getBody();
    }

    /**
     * 构建完整内部调用地址。
     *
     * @param path 接口路径
     * @return URI
     */
    private URI buildUri(String path) {
        return UriComponentsBuilder.fromHttpUrl(properties.resolveSystemBaseUrl())
                .path(path)
                .build(true)
                .toUri();
    }

    /**
     * 将 ID 集合转为逗号分隔字符串。
     *
     * @param ids ID 集合
     * @return Optional 包装的字符串
     */
    private Optional<String> joinIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Optional.empty();
        }
        String joined = ids.stream()
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? Optional.empty() : Optional.of(joined);
    }
}
