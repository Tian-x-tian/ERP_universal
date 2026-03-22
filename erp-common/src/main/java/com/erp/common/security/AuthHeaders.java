package com.erp.common.security;

import java.util.List;

/**
 * 统一定义网关向下游透传的内部认证请求头。
 */
public final class AuthHeaders {
    public static final String USER_ID = "X-Auth-User-Id";
    public static final String USER_NAME = "X-Auth-User-Name";
    public static final String TENANT_ID = "X-Auth-Tenant-Id";
    public static final String TOKEN_VERSION = "X-Auth-Token-Version";
    public static final String EXPIRES_AT = "X-Auth-Expires-At";
    public static final String SIGNATURE = "X-Auth-Signature";

    public static final List<String> INTERNAL_HEADERS = List.of(
            USER_ID,
            USER_NAME,
            TENANT_ID,
            TOKEN_VERSION,
            EXPIRES_AT,
            SIGNATURE);

    private AuthHeaders() {
    }
}
