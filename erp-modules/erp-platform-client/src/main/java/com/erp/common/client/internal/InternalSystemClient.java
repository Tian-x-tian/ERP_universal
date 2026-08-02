package com.erp.common.client.internal;

import com.erp.platform.contract.model.PlatformAuthorityBundle;
import com.erp.platform.contract.model.PlatformAiActionPolicyItem;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiAuditView;
import com.erp.platform.contract.model.PlatformAiConfigUpdateRequest;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.platform.contract.model.PlatformImexJob;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;
import com.erp.platform.contract.model.PlatformItemView;
import com.erp.platform.contract.model.PlatformNoticeCreateRequest;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.platform.contract.model.PlatformWarehouseView;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * 平台内部接口客户端。
 */
@Component
public class InternalSystemClient {
    private final RestTemplate restTemplate;
    private final InternalRequestHeaderFactory headerFactory;
    private final InternalSystemClientProperties properties;

    public InternalSystemClient(RestTemplate internalSystemRestTemplate,
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

    public PlatformUserView getUserById(Long userId) {
        return exchange(buildUri("/system/internal/platform/users/" + userId), HttpMethod.GET, null,
                PlatformUserView.class);
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

    public com.erp.platform.contract.model.PlatformNoticeView getNotice(Long noticeId) {
        return exchange(buildUri("/system/internal/platform/notices/" + noticeId), HttpMethod.GET, null,
                com.erp.platform.contract.model.PlatformNoticeView.class);
    }

    public Boolean markNoticeRead(Long noticeId, Long userId) {
        return exchange(
                UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/notices/" + noticeId + "/read"))
                        .queryParam("userId", userId).build(true).toUri(),
                HttpMethod.POST, null, Boolean.class);
    }

    public Integer markAllNoticeRead(Long userId) {
        return exchange(UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/notices/read-all"))
                .queryParam("userId", userId).build(true).toUri(), HttpMethod.POST, null, Integer.class);
    }

    public Boolean hasPermission(String permission) {
        return exchange(UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/permissions/check"))
                .queryParam("permission", permission).build(true).toUri(), HttpMethod.GET, null, Boolean.class);
    }

    public Long countUnreadNotices(Long userId) {
        return exchange(UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/notices/unread-count"))
                .queryParam("userId", userId).build(true).toUri(), HttpMethod.GET, null, Long.class);
    }

    public List<com.erp.platform.contract.model.PlatformNoticeView> getLatestNotices(Long userId, int limit) {
        ResponseEntity<List<com.erp.platform.contract.model.PlatformNoticeView>> response = restTemplate.exchange(
                UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/notices/latest"))
                        .queryParam("userId", userId)
                        .queryParam("limit", limit)
                        .build(true)
                        .toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<com.erp.platform.contract.model.PlatformNoticeView>>() {
                });
        List<com.erp.platform.contract.model.PlatformNoticeView> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 查询当前租户 AI 配置。
     *
     * @return AI 配置
     */
    public PlatformAiConfigView getAiConfig() {
        return exchange(buildUri("/system/internal/platform/ai/config"), HttpMethod.GET, null, PlatformAiConfigView.class);
    }

    /**
     * 更新当前租户 AI 配置。
     *
     * @param request 更新请求
     * @return 更新后的 AI 配置
     */
    public PlatformAiConfigView updateAiConfig(PlatformAiConfigUpdateRequest request) {
        return exchange(buildUri("/system/internal/platform/ai/config"), HttpMethod.PUT, request, PlatformAiConfigView.class);
    }

    /**
     * 查询当前租户 AI 动作策略。
     *
     * @return 动作策略列表
     */
    public List<PlatformAiActionPolicyItem> listAiActionPolicies() {
        ResponseEntity<List<PlatformAiActionPolicyItem>> response = restTemplate.exchange(
                buildUri("/system/internal/platform/ai/policy/actions"),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<PlatformAiActionPolicyItem>>() {
                });
        List<PlatformAiActionPolicyItem> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 更新当前租户 AI 动作策略。
     *
     * @param policyItems 策略列表
     * @return 更新后的策略列表
     */
    public List<PlatformAiActionPolicyItem> updateAiActionPolicies(List<PlatformAiActionPolicyItem> policyItems) {
        ResponseEntity<List<PlatformAiActionPolicyItem>> response = restTemplate.exchange(
                buildUri("/system/internal/platform/ai/policy/actions"),
                HttpMethod.PUT,
                new HttpEntity<>(policyItems, headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<PlatformAiActionPolicyItem>>() {
                });
        List<PlatformAiActionPolicyItem> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 写入 AI 审计记录。
     *
     * @param request 审计写入请求
     */
    public void recordAiAudit(PlatformAiAuditCreateRequest request) {
        exchange(buildUri("/system/internal/platform/ai/audit"), HttpMethod.POST, request, Void.class);
    }

    /**
     * Applies a quota reservation, settlement, or release in the tenant-local counter.
     *
     * @param event idempotent quota event
     * @return current local quota usage
     */
    public SaasQuotaUsage applyQuotaEvent(SaasUsageEvent event) {
        return exchange(buildUri("/system/internal/saas/quotas/events"), HttpMethod.POST, event,
                SaasQuotaUsage.class);
    }

    /**
     * Applies multiple local quota events in one system-service transaction.
     *
     * @param events quota events
     * @return current usages in request order
     */
    public List<SaasQuotaUsage> applyQuotaEvents(List<SaasUsageEvent> events) {
        ResponseEntity<List<SaasQuotaUsage>> response = restTemplate.exchange(
                buildUri("/system/internal/saas/quotas/events/batch"),
                HttpMethod.POST,
                new HttpEntity<>(events, headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<SaasQuotaUsage>>() {
                });
        List<SaasQuotaUsage> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * Initialize a tenant's local system data.
     *
     * @param request initialization request
     * @return initialization result
     */
    public SaasTenantInitializationResult initializeSaasTenant(SaasTenantInitializationRequest request) {
        return exchange(buildUri("/system/internal/saas/tenants/initialize"),
                HttpMethod.POST,
                request,
                SaasTenantInitializationResult.class);
    }

    /**
     * Reissues the one-time activation token after a successful tenant initialization replay.
     *
     * @param request activation reissue request
     * @return replacement one-time token
     */
    public SaasTenantActivationReissueResult reissueSaasTenantActivation(
            SaasTenantActivationReissueRequest request) {
        return exchange(buildUri("/system/internal/saas/tenants/activation/reissue"),
                HttpMethod.POST,
                request,
                SaasTenantActivationReissueResult.class);
    }

    /**
     * Returns the local signed-snapshot access decision for the current tenant.
     *
     * @return current runtime access decision
     */
    public SaasRuntimeAccess getSaasRuntimeAccess() {
        return exchange(buildUri("/system/internal/saas/runtime-access"),
                HttpMethod.GET, null, SaasRuntimeAccess.class);
    }

    /**
     * Irreversibly removes all local data for a confirmed tenant purge.
     *
     * @param request confirmed purge request
     * @return deletion summary
     */
    public SaasTenantPurgeResult purgeSaasTenant(SaasTenantPurgeRequest request) {
        return exchange(buildUri("/system/internal/saas/tenants/purge"),
                HttpMethod.POST, request, SaasTenantPurgeResult.class);
    }

    /**
     * 查询当前租户 AI 审计记录。
     *
     * @param limit 限制条数
     * @return 审计记录列表
     */
    public List<PlatformAiAuditView> listAiAuditRecords(int limit) {
        ResponseEntity<List<PlatformAiAuditView>> response = restTemplate.exchange(
                UriComponentsBuilder.fromUri(buildUri("/system/internal/platform/ai/audit"))
                        .queryParam("limit", limit)
                        .build(true)
                        .toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<PlatformAiAuditView>>() {
                });
        List<PlatformAiAuditView> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
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
}
