package com.erp.saas.control.controller;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.domain.model.ResolvedTenantDomain;
import com.erp.saas.control.service.snapshot.SaasEntitlementSnapshotService;
import com.erp.saas.control.service.usage.SaasUsageAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalSaasControllerTest {
    @Test
    void shouldReturnOnlyVerifiedResolutionAndHideMissAsNotFound() {
        SaasDomainService domains = mock(SaasDomainService.class);
        SaasEntitlementSnapshotService snapshots = mock(SaasEntitlementSnapshotService.class);
        SaasUsageAggregationService usage = mock(SaasUsageAggregationService.class);
        InternalSaasController controller = new InternalSaasController(domains, snapshots, usage);
        when(domains.resolve("Acme.example")).thenReturn(Optional.of(new ResolvedTenantDomain(
                1L, "tenant_1", "acme.example", DeploymentMode.SHARED, TenantLifecycleState.ACTIVE)));

        var resolved = controller.resolveHost("Acme.example");
        assertThat(resolved.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resolved.getBody()).extracting("host", "tenantId", "deploymentMode", "verified")
                .containsExactly("acme.example", "tenant_1", DeploymentMode.SHARED, true);

        when(domains.resolve("missing.example")).thenReturn(Optional.empty());
        assertThat(controller.resolveHost("missing.example").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldAuditSnapshotLeaseToAuthenticatedServicePrincipal() {
        SaasDomainService domains = mock(SaasDomainService.class);
        SaasEntitlementSnapshotService snapshots = mock(SaasEntitlementSnapshotService.class);
        SaasUsageAggregationService usage = mock(SaasUsageAggregationService.class);
        InternalSaasController controller = new InternalSaasController(domains, snapshots, usage);
        SaasEntitlementSnapshot expected = new SaasEntitlementSnapshot();
        when(snapshots.load("tenant_1", "erp-gateway")).thenReturn(expected);

        var principal = new AuthenticatedUserPrincipal(0L, "erp-gateway", "000000", 0, 4102444800000L);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, "n/a", java.util.List.of());
        assertThat(controller.entitlementSnapshot("tenant_1", authentication)).isSameAs(expected);
        verify(snapshots).load("tenant_1", "erp-gateway");
    }

    @Test
    void shouldAggregateUsageAsAuthenticatedServicePrincipal() {
        SaasDomainService domains = mock(SaasDomainService.class);
        SaasEntitlementSnapshotService snapshots = mock(SaasEntitlementSnapshotService.class);
        SaasUsageAggregationService usage = mock(SaasUsageAggregationService.class);
        InternalSaasController controller = new InternalSaasController(domains, snapshots, usage);
        SaasUsageEvent event = new SaasUsageEvent("event-a", "tenant_1", SaasQuotaKeys.USER_COUNT,
                SaasUsageOperation.REPORT, null, 5L, null, 1L);
        var principal = new AuthenticatedUserPrincipal(0L, "erp-system", "000000", 0, 4102444800000L);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, "n/a", java.util.List.of());

        assertThat(controller.reportUsage(event, authentication).getStatusCode().value()).isEqualTo(204);
        verify(usage).report(event, "erp-system");
    }
}
