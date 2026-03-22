package com.erp.business.security.filter;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JWT 认证过滤器单元测试。
 */
class JwtAuthenticationFilterTest {
    private static final String INTERNAL_SECRET = "test-internal-secret";

    /**
     * 清理线程上下文，避免测试相互污染。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    /**
     * 验证有效令牌在业务模块中可以直接完成认证。
     *
     * @throws Exception 过滤器执行异常
     */
    @Test
    void shouldAuthenticateBusinessRequestByJwtClaims() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new ObjectMapper(), INTERNAL_SECRET);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(1001L, "tester", "TENANT_A", 3,
                System.currentTimeMillis() + 60000);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/business/inventory/ledger/list");
        request.addHeader("tenantId", "TENANT_A");
        request.addHeader(AuthHeaders.USER_ID, String.valueOf(principal.getUserId()));
        request.addHeader(AuthHeaders.USER_NAME, principal.getUserName());
        request.addHeader(AuthHeaders.TENANT_ID, principal.getTenantId());
        request.addHeader(AuthHeaders.TOKEN_VERSION, String.valueOf(principal.getTokenVersion()));
        request.addHeader(AuthHeaders.EXPIRES_AT, String.valueOf(principal.getExpiresAt()));
        request.addHeader(AuthHeaders.SIGNATURE, InternalAuthSignatureUtils.sign(INTERNAL_SECRET, principal));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, buildFilterChain(chainInvoked));

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(chainInvoked.get());
    }

    /**
     * 验证租户与令牌不匹配时返回 401。
     *
     * @throws Exception 过滤器执行异常
     */
    @Test
    void shouldRejectRequestWhenTenantHeaderDoesNotMatchToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new ObjectMapper(), INTERNAL_SECRET);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(1001L, "tester", "TENANT_A", 1,
                System.currentTimeMillis() + 60000);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/business/inventory/ledger/list");
        request.addHeader("tenantId", "TENANT_B");
        request.addHeader(AuthHeaders.USER_ID, String.valueOf(principal.getUserId()));
        request.addHeader(AuthHeaders.USER_NAME, principal.getUserName());
        request.addHeader(AuthHeaders.TENANT_ID, principal.getTenantId());
        request.addHeader(AuthHeaders.TOKEN_VERSION, String.valueOf(principal.getTokenVersion()));
        request.addHeader(AuthHeaders.EXPIRES_AT, String.valueOf(principal.getExpiresAt()));
        request.addHeader(AuthHeaders.SIGNATURE, InternalAuthSignatureUtils.sign(INTERNAL_SECRET, principal));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, buildFilterChain(new AtomicBoolean(false)));

        Assertions.assertEquals(401, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("租户与令牌不匹配"));
    }

    /**
     * 构造可观测的过滤器链。
     *
     * @param chainInvoked 是否已进入下游链路
     * @return 过滤器链
     */
    private FilterChain buildFilterChain(AtomicBoolean chainInvoked) {
        return (request, response) -> {
            chainInvoked.set(true);
            ((MockHttpServletResponse) response).setStatus(200);
        };
    }
}
