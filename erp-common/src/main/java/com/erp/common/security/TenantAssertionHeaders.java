package com.erp.common.security;

import java.util.List;

/**
 * Headers used for the gateway-resolved tenant assertion.
 */
public final class TenantAssertionHeaders {
    public static final String TENANT_ID = "X-Resolved-Tenant-Id";
    public static final String HOST = "X-Resolved-Tenant-Host";
    public static final String METHOD = "X-Resolved-Tenant-Method";
    public static final String PATH = "X-Resolved-Tenant-Path";
    public static final String ISSUED_AT = "X-Resolved-Tenant-Issued-At";
    public static final String NONCE = "X-Resolved-Tenant-Nonce";
    public static final String SIGNATURE = "X-Resolved-Tenant-Signature";

    public static final List<String> INTERNAL_HEADERS = List.of(
            TENANT_ID, HOST, METHOD, PATH, ISSUED_AT, NONCE, SIGNATURE);

    private TenantAssertionHeaders() {
    }
}
