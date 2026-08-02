package com.erp.common.client.internal;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthContextHolder;
import com.erp.common.security.InternalAuthSignatureUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 平台内部调用请求头构造器。
 */
@Component
public class InternalRequestHeaderFactory {
    private final InternalSystemClientProperties properties;

    public InternalRequestHeaderFactory(InternalSystemClientProperties properties) {
        this.properties = properties;
    }

    /**
     * 构建带内部签名的请求头。
     *
     * @return 请求头
     */
    public HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        HttpServletRequest currentRequest = currentRequest();
        if (hasForwardableAuthHeaders(currentRequest)) {
            copyForwardableHeaders(currentRequest, headers);
            return headers;
        }
        // 异步线程上没有 Servlet 请求绑定，回退读取线程内的认证头快照，保持“真实用户”身份。
        if (InternalAuthContextHolder.hasForwardableHeaders()) {
            copyForwardableHeaders(InternalAuthContextHolder.get(), headers);
            return headers;
        }

        AuthenticatedUserPrincipal principal = buildServicePrincipal();
        headers.set(AuthHeaders.USER_ID, String.valueOf(principal.getUserId()));
        headers.set(AuthHeaders.USER_NAME, principal.getUserName());
        headers.set(AuthHeaders.TENANT_ID, principal.getTenantId());
        headers.set(AuthHeaders.TOKEN_VERSION, String.valueOf(principal.getTokenVersion()));
        headers.set(AuthHeaders.EXPIRES_AT, String.valueOf(principal.getExpiresAt()));
        headers.set(AuthHeaders.SIGNATURE,
                InternalAuthSignatureUtils.sign(properties.resolveAuthSignatureSecret(),
                        principal.getUserId(),
                        principal.getUserName(),
                        principal.getTenantId(),
                        principal.getTokenVersion(),
                        principal.getExpiresAt()));
        headers.set("tenantId", principal.getTenantId());
        return headers;
    }

    /**
     * 构建兜底服务主体。
     *
     * @return 服务主体
     */
    private AuthenticatedUserPrincipal buildServicePrincipal() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            tenantId = properties.getServiceTenantId();
        }
        return new AuthenticatedUserPrincipal(
                properties.getServiceUserId(),
                properties.getServiceUserName(),
                StringUtils.hasText(tenantId) ? tenantId.trim() : "000000",
                properties.getServiceTokenVersion(),
                properties.getServiceExpiresAt());
    }

    /**
     * 判断当前请求是否可直接透传内部认证头。
     *
     * @param request 当前请求
     * @return true 表示可透传
     */
    private boolean hasForwardableAuthHeaders(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return StringUtils.hasText(request.getHeader(AuthHeaders.USER_ID))
                && StringUtils.hasText(request.getHeader(AuthHeaders.USER_NAME))
                && StringUtils.hasText(request.getHeader(AuthHeaders.TENANT_ID))
                && StringUtils.hasText(request.getHeader(AuthHeaders.EXPIRES_AT))
                && StringUtils.hasText(request.getHeader(AuthHeaders.SIGNATURE));
    }

    /**
     * 复制当前请求中的内部认证头。
     *
     * @param request 当前请求
     * @param headers 目标请求头
     */
    private void copyForwardableHeaders(HttpServletRequest request, HttpHeaders headers) {
        for (String headerName : AuthHeaders.INTERNAL_HEADERS) {
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                headers.set(headerName, headerValue.trim());
            }
        }
        String tenantId = request.getHeader("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getHeader(AuthHeaders.TENANT_ID);
        }
        if (StringUtils.hasText(tenantId)) {
            headers.set("tenantId", tenantId.trim());
        }
    }

    /**
     * 复制线程内认证头快照。
     *
     * @param snapshot 认证头快照
     * @param headers  目标请求头
     */
    private void copyForwardableHeaders(Map<String, String> snapshot, HttpHeaders headers) {
        for (String headerName : AuthHeaders.INTERNAL_HEADERS) {
            String headerValue = snapshot.get(headerName);
            if (StringUtils.hasText(headerValue)) {
                headers.set(headerName, headerValue.trim());
            }
        }
        String tenantId = snapshot.get(AuthHeaders.TENANT_ID);
        if (StringUtils.hasText(tenantId)) {
            headers.set("tenantId", tenantId.trim());
        }
    }

    /**
     * 获取当前线程绑定的 Servlet 请求。
     *
     * @return 当前请求
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
