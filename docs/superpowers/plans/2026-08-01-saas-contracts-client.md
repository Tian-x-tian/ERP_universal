# SaaS Contracts and Internal Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Follow superpowers:test-driven-development and do not add the control service in this task.

**Goal:** Freeze the login-precondition tenant assertion and the typed control-plane contracts/client without introducing a runtime dependency on a not-yet-created control service.

**Architecture:** Login-precondition assertions form a separate HMAC security domain from authenticated `AuthHeaders`. SaaS DTOs live in a POJO-only contract module, while a small typed client under the existing `com.erp.common` component-scan root uses signed service-principal headers for `/internal/saas/**` calls. The reactive Gateway will not use this `RestTemplate` client; it will later depend on the contract module and implement a non-blocking resolver with `WebClient`.

**Tech Stack:** Java 17, HmacSHA256, Jackson annotations, Spring RestTemplate, JUnit 5, Mockito.

---

### Task 1: Resolved-tenant assertion contract

**Files:**
- Create: `erp-common/src/main/java/com/erp/common/security/TenantAssertionHeaders.java`
- Create: `erp-common/src/main/java/com/erp/common/security/ResolvedTenantAssertion.java`
- Create: `erp-common/src/main/java/com/erp/common/security/ResolvedTenantAssertionSignatureUtils.java`
- Create: `erp-common/src/test/java/com/erp/common/security/ResolvedTenantAssertionSignatureUtilsTest.java`

- [ ] Add tests `shouldSignAndVerifyResolvedTenantAssertion`, `shouldMatchFrozenHmacGoldenVector`, `shouldRejectEveryModifiedSignedField`, `shouldTreatNonblankSecretWhitespaceAsKeyMaterial`, `shouldRejectExpiredAssertion`, `shouldRejectFutureAssertion`, `shouldRejectMalformedSignature`, `shouldRejectCrLfInSignedFields`, and `shouldNormalizeHostMethodAndPath`. Expected RED: all three production types are absent.
- [ ] Freeze headers as `X-Resolved-Tenant-Id`, `-Host`, `-Method`, `-Path`, `-Issued-At`, `-Nonce`, and `-Signature`, exposed through one immutable `INTERNAL_HEADERS` list for gateway stripping.
- [ ] Define assertion fields `tenantId`, `host`, `method`, `path`, epoch-millisecond `issuedAt`, and `nonce`. The canonical UTF-8 payload is exactly seven newline-separated values: fixed domain/version line `ERP-SAAS-TENANT-ASSERTION-V1`, followed by the six normalized values in that order.
- [ ] Normalize tenant ID by trimming; host by parsing an authority-only URI, trim/lowercase with `Locale.ROOT`, remove a valid port and one trailing dot; method by trim/uppercase; path by `URI.create`, rejecting query/fragment/authority, normalizing dot segments, preserving percent escapes as raw path, and requiring one leading slash. Do not collapse repeated slashes or percent-decode. Reject blank values, CR/LF, invalid ports/URIs, and missing nonce/time.
- [ ] Sign with HmacSHA256 and URL-safe Base64 without padding. The secret is supplied from the separate configuration key `erp.saas.tenant-assertion-signature-secret`; the utility rejects a null/blank secret but uses every byte of a nonblank secret exactly as configured (no trim), and never falls back to `erp.internal.auth-signature-secret`. Verify decoded bytes with `MessageDigest.isEqual`; allow only the closed freshness interval `[now-30s, now+30s]`. Freeze one independently computed canonical-payload/signature vector and test every signed field mutation. Do not reuse or change `AuthHeaders`/`InternalAuthSignatureUtils`; nonce generation/Redis consumption remains out of scope.
- [ ] Run `mvn -pl erp-common -Dtest=ResolvedTenantAssertionSignatureUtilsTest test` for RED then GREEN.

### Task 2: POJO-only SaaS contract module

**Files:**
- Modify: `erp-modules/pom.xml` to add `erp-saas-contract` before client modules
- Create: `erp-modules/erp-saas-contract/pom.xml`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/TenantLifecycleState.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/DeploymentMode.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SubscriptionState.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasUsageOperation.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasFeatureGrant.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasQuotaLimit.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasQuotaUsage.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasQuotaKeys.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasHostResolution.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasUsageEvent.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasUsageEventValidator.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasProvisioningRequest.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasProvisioningResult.java`
- Create: `erp-modules/erp-saas-contract/src/main/java/com/erp/saas/contract/model/SaasEntitlementSnapshot.java`
- Create: `erp-modules/erp-saas-contract/src/test/java/com/erp/saas/contract/model/SaasContractTest.java`

- [ ] First create `erp-saas-contract/pom.xml` with only `jackson-annotations` plus test starter and add the module to `erp-modules/pom.xml`; then add a Jackson JSON round-trip/reflection test proving contract classes are serializable mutable beans, contain no MyBatis/Spring annotations, use `@JsonIgnoreProperties(ignoreUnknown = true)`, preserve unknown-field compatibility with a default `ObjectMapper`, and expose all required enum values. Run `mvn -pl erp-modules/erp-saas-contract -am -Dtest=SaasContractTest test`; expected RED is missing production types, never “selected project not found”.
- [ ] Define `TenantLifecycleState`: `DRAFT`, `PROVISIONING`, `TRIAL`, `ACTIVE`, `GRACE`, `READ_ONLY`, `ARCHIVED`, `PURGE_PENDING`, `PURGED`, `SUSPENDED`, `PROVISION_FAILED`.
- [ ] Define `DeploymentMode`: `SHARED`, `DEDICATED`; and `SubscriptionState`: `TRIAL`, `ACTIVE`, `GRACE`, `EXPIRED`, `CANCELED`.
- [ ] Define `SaasUsageOperation`: `RESERVE`, `SETTLE`, `RELEASE`, `REPORT`.
- [ ] Freeze quota keys in non-instantiable `SaasQuotaKeys`: `user_count`, `storage_bytes`, `ai_input_tokens`, and `ai_output_tokens`. The first two are non-periodic; both AI counters reset monthly and use the same UTC month-start period.
- [ ] Add mutable beans with exact Java types: `SaasFeatureGrant(String featureKey, boolean granted)`; `SaasQuotaLimit(String quotaKey, Long limit)` where only null means unlimited and zero means no allocation; `SaasQuotaUsage(String quotaKey, long used, long reserved, Long periodStartEpochMs)` where non-periodic user/storage metrics use null and monthly AI uses UTC month-start; `SaasHostResolution(String host, String tenantId, DeploymentMode deploymentMode, TenantLifecycleState lifecycleState, boolean verified)`; `SaasUsageEvent(String idempotencyKey, String tenantId, String metricKey, SaasUsageOperation operation, String referenceKey, Long amount, Long periodStartEpochMs, long occurredAtEpochMs)`; `SaasProvisioningRequest(String requestId, String tenantId, DeploymentMode deploymentMode, String planCode)`; and `SaasProvisioningResult(String requestId, String tenantId, boolean success, String message, boolean activationRequired, long completedAtEpochMs)`.
- [ ] Add `SaasEntitlementSnapshot` with `String tenantId`, lifecycle/deployment/subscription enums, `String planCode`, `long version`, `long issuedAtEpochMs`, `long expiresAtEpochMs`, `List<SaasFeatureGrant> featureGrants`, `List<SaasQuotaLimit> quotaLimits`, `String signatureKeyId`, and `String signature`. Lists default to empty mutable lists and are never serialized as null. The versioned HmacSHA256 canonical payload/signature verifier is implemented with entitlement enforcement later; key material is never part of this contract.
- [ ] Freeze usage semantics and implement their stateless boundary rules in `SaasUsageEventValidator`: `RESERVE` requires `amount > 0` and creates one outstanding reservation identified by `referenceKey`; `SETTLE` requires the same reference and `amount >= 0`, consumes the entire outstanding reservation, and increases used by the actual amount (the later stateful service rejects actual above reserved); `RELEASE` requires the same reference and `amount == null`, consumes the entire outstanding reservation without changing used; `REPORT` requires no reference and `amount >= 0`, and is an absolute used-value reconciliation snapshot, not a delta. A report is applied only if its `occurredAtEpochMs` is newer than the last report for the same tenant/metric/period; duplicate `idempotencyKey` is always a no-op, while reuse with a different payload is a conflict. `user_count`/`storage_bytes` events require `periodStartEpochMs == null`; `ai_input_tokens`/`ai_output_tokens` require the UTC first-day 00:00:00.000 month start; unknown metric keys fail validation. Add positive/negative boundary tests and JSON round trips for every operation; the stateful reservation amount and event ordering rules are repeated in the later control-service aggregation tests.
- [ ] Other required strings/enums are non-null at service boundaries. `message` is the only nullable provisioning-result field: failure requires a nonblank message; success may set it null. The provisioning result never carries an activation token or URL: a later one-time activation flow generates raw credentials only at delivery time and stores only their hash, so idempotent retries never require persisted plaintext. Timestamps are epoch milliseconds throughout; counters are `long`; collection order is contract-significant and must be stable when snapshot signing is implemented.
- [ ] Run `mvn -pl erp-modules/erp-saas-contract -am test`.

### Task 3: Typed internal SaaS client

**Files:**
- Modify: root `pom.xml` dependency management for `erp-saas-client`
- Modify: `erp-modules/pom.xml` to add `erp-saas-client` after contract/internal-client-core
- Modify: `erp-modules/erp-internal-client-core/src/main/java/com/erp/common/client/internal/InternalSystemClientProperties.java`
- Modify: `erp-modules/erp-internal-client-core/src/main/java/com/erp/common/client/internal/InternalSystemClientConfig.java`
- Modify: `erp-modules/erp-internal-client-core/src/main/java/com/erp/common/client/internal/InternalRequestHeaderFactory.java`
- Modify: `erp-modules/erp-internal-client-core/src/test/java/com/erp/common/client/internal/InternalClientCoreSupportTest.java`
- Create: `erp-modules/erp-saas-client/pom.xml`
- Create: `erp-modules/erp-saas-client/src/main/java/com/erp/common/client/internal/SaasInternalClientConfig.java`
- Create: `erp-modules/erp-saas-client/src/main/java/com/erp/common/client/internal/InternalSaasClient.java`
- Create: `erp-modules/erp-saas-client/src/test/java/com/erp/common/client/internal/SaasInternalClientConfigTest.java`
- Create: `erp-modules/erp-saas-client/src/test/java/com/erp/common/client/internal/InternalSaasClientTest.java`

- [ ] Extend `shouldFallbackToServiceNameBaseUrlsWhenBlankConfigured` to expect blank SaaS URL resolves to `http://erp-saas-control`; add positive finite defaults `saasConnectTimeoutMs=2000` and `saasReadTimeoutMs=5000` and reject zero/negative configured values. Observe compile RED, then add these fields/resolvers without renaming the shared properties class.
- [ ] First create `erp-saas-client/pom.xml` with explicit `${project.version}` dependencies on `erp-internal-client-core` and `erp-saas-contract` plus test starter, add it after both dependencies in `erp-modules/pom.xml`, and add root dependency management for `erp-saas-client`. Then add client tests `shouldResolveTenantByHost`, `shouldEncodeHostQuery`, `shouldLoadEntitlementSnapshot`, `shouldReportUsageWithIdempotencyKey`, `shouldReportProvisioningResult`, `shouldRejectBlankInputs`, `shouldRejectEmptyResponseBody`, `shouldPropagateNotFound`, `shouldPropagateServiceUnavailable`, and `shouldPropagateReportingFailure`. Run `mvn -pl erp-modules/erp-saas-client -am -Dtest=InternalSaasClientTest test`; expected RED is the missing client type, never “selected project not found”. Use existing Mockito `RestTemplate` + `ArgumentCaptor<URI>` style and verify `InternalRequestHeaderFactory.buildHeaders()`.
- [ ] Freeze routes: GET `/internal/saas/hosts/resolve?host={host}`, GET `/internal/saas/tenants/{tenantId}/entitlement-snapshot`, POST `/internal/saas/usage-events`, POST `/internal/saas/provisioning/results`.
- [ ] Add `SaasInternalClientConfig` with a `@LoadBalanced` bean named `saasInternalRestTemplate`. Build it from `RestTemplateBuilder` and a JDK `HttpClient` configured with `Redirect.NEVER` plus the resolved 2-second connect timeout; set the JDK request factory's read timeout to 5 seconds. In `SaasInternalClientConfigTest`, verify finite timeouts, `@LoadBalanced`, and use a real loopback `HttpServer` whose `/redirect` returns `302 Location: /target`: the client must surface the 302 through the strict error handler and `/target` must receive zero requests. This test must use the real request factory, not `MockRestServiceServer`.
- [ ] Mark the existing `internalSystemRestTemplate` bean `@Primary` so adding the qualified SaaS template cannot make existing unqualified platform/workflow/business client constructors ambiguous. Add a context/reflection regression proving the primary bean is selected when both templates exist.
- [ ] Configure the SaaS template with a dedicated `DefaultResponseErrorHandler` override whose `hasError(HttpStatusCode)` returns true for every non-2xx status, including 3xx. Test GET and POST 3xx failure; no redirect/non-2xx may be treated as successful reporting.
- [ ] Add `InternalRequestHeaderFactory.buildServiceHeaders()` that always creates the configured service principal, including configured service tenant `000000`, and never reads `RequestContextHolder` or `TenantContextHolder`. Preserve existing `buildHeaders()` behavior for current callers. Client constructor receives `@Qualifier("saasInternalRestTemplate") RestTemplate`, the header factory, and properties and uses only `buildServiceHeaders()`. Add a test binding both an end-user servlet request and tenant context and prove the SaaS call still sends the configured service principal/signature.
- [ ] The client class is a Spring `@Component` under `com.erp.common.client.internal`. Validate required inputs before constructing URIs and use `UriComponentsBuilder` so host/query/path values are encoded exactly once. Return DTOs directly and fail fast with `IllegalStateException` on a successful response with an empty body; return void for reporting calls.
- [ ] Freeze server/client failure semantics: unknown or unverified host and missing snapshot are HTTP 404; control 5xx/timeouts and all non-2xx responses propagate unchanged; invalid snapshot signatures are rejected later by the local snapshot verifier, not by this transport client; duplicate idempotent report posts return any 2xx and are success. The client performs no internal retry, catches no transport exception, and creates no fallback/default authorization data.
- [ ] Keep client dependencies limited to `erp-internal-client-core`, `erp-saas-contract`, and tests. Do not add `erp-saas-control` to the reactor in this task. Reactive Gateway code must not depend on this module.
- [ ] Run `mvn -pl erp-modules/erp-saas-client -am test`, then full `mvn test`, `git diff --check`, inspect status, and commit `feat: add SaaS contracts and internal client` without staging coordination docs.
