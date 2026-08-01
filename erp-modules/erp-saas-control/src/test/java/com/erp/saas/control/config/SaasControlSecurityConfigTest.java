package com.erp.saas.control.config;

import com.erp.common.core.domain.R;
import com.erp.common.security.AuthHeaders;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.common.security.InternalAuthSignatureUtils;
import com.erp.saas.control.security.SaasAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "erp.saas.sql.upgrade.enabled=false",
        "erp.saas.schema-validation.enabled=false",
        "erp.internal.auth-signature-secret=test-only-internal-secret"
})
@AutoConfigureMockMvc
@Import({SaasControlSecurityConfigTest.TestController.class, SaasControlSecurityConfigTest.FixedClockConfig.class})
class SaasControlSecurityConfigTest {
    private static final String SECRET = "test-only-internal-secret";
    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

    @MockBean
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasAuthenticationFilter authenticationFilter;

    @Test
    void shouldReturnJsonUnauthorizedWithoutCredentialsForBothRouteGroups() throws Exception {
        assertUnauthorized("/saas/ping");
        assertUnauthorized("/internal/saas/ping");
    }

    @Test
    void shouldAcceptValidSignedPrincipalForBothRouteGroups() throws Exception {
        performValidSigned("/saas/ping")
                .andExpect(status().isOk());
        performValidSigned("/internal/saas/ping")
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidSignatureForBothRouteGroups() throws Exception {
        performSigned("/saas/ping", "invalid-signature")
                .andExpect(status().isUnauthorized());
        performSigned("/internal/saas/ping", "invalid-signature")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpiredSignedPrincipalForBothRouteGroups() throws Exception {
        AuthenticatedUserPrincipal expired = principal(NOW.toEpochMilli() - 1);
        String signature = InternalAuthSignatureUtils.sign(SECRET, expired);

        performSigned("/saas/ping", expired, signature)
                .andExpect(status().isUnauthorized());
        performSigned("/internal/saas/ping", expired, signature)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotClaimSimilarOrUnrelatedPaths() throws Exception {
        mockMvc.perform(get("/saasx/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/internal/saasx/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/unrelated/ping")).andExpect(status().isOk());
    }

    @Test
    void shouldMatchExactGroupsAfterRemovingContextPath() {
        assertThat(requiresAuth("/erp/saas", "/erp")).isTrue();
        assertThat(requiresAuth("/erp/saas/item", "/erp")).isTrue();
        assertThat(requiresAuth("/erp/internal/saas", "/erp")).isTrue();
        assertThat(requiresAuth("/erp/internal/saas/item", "/erp")).isTrue();
        assertThat(requiresAuth("/erp/saasx", "/erp")).isFalse();
        assertThat(requiresAuth("/erp/internal/saasx", "/erp")).isFalse();
    }

    private void assertUnauthorized(String path) throws Exception {
        String content = mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        assertThat(json.path("code").asLong()).isEqualTo(40101L);
    }

    private org.springframework.test.web.servlet.ResultActions performSigned(String path, String signature)
            throws Exception {
        AuthenticatedUserPrincipal principal = principal();
        return performSigned(path, principal, signature);
    }

    private org.springframework.test.web.servlet.ResultActions performValidSigned(String path) throws Exception {
        AuthenticatedUserPrincipal principal = principal();
        return performSigned(path, principal, InternalAuthSignatureUtils.sign(SECRET, principal));
    }

    private org.springframework.test.web.servlet.ResultActions performSigned(String path,
            AuthenticatedUserPrincipal principal,
            String signature) throws Exception {
        return mockMvc.perform(get(path)
                .header(AuthHeaders.USER_ID, principal.getUserId())
                .header(AuthHeaders.USER_NAME, principal.getUserName())
                .header(AuthHeaders.TENANT_ID, principal.getTenantId())
                .header(AuthHeaders.TOKEN_VERSION, principal.getTokenVersion())
                .header(AuthHeaders.EXPIRES_AT, principal.getExpiresAt())
                .header(AuthHeaders.SIGNATURE, signature));
    }

    private AuthenticatedUserPrincipal principal() {
        return principal(NOW.toEpochMilli() + 60_000L);
    }

    private AuthenticatedUserPrincipal principal(long expiresAt) {
        return new AuthenticatedUserPrincipal(1L, "admin", "000000", 0, expiresAt);
    }

    private boolean requiresAuth(String requestUri, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setContextPath(contextPath);
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(authenticationFilter, "requiresAuth", request));
    }

    @RestController
    static class TestController {
        @GetMapping(value = {
                "/saas/ping",
                "/internal/saas/ping",
                "/saasx/ping",
                "/internal/saasx/ping",
                "/unrelated/ping"
        }, produces = MediaType.APPLICATION_JSON_VALUE)
        R<String> ping() {
            return R.success("ok");
        }
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
