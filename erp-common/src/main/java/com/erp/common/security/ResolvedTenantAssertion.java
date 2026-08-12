package com.erp.common.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable normalized tenant context asserted by the gateway before login.
 */
public final class ResolvedTenantAssertion {
    private final String tenantId;
    private final String host;
    private final String method;
    private final String path;
    private final long issuedAt;
    private final String nonce;

    public ResolvedTenantAssertion(String tenantId, String host, String method, String path, long issuedAt,
            String nonce) {
        this.tenantId = normalizeText(tenantId, "tenantId");
        this.host = normalizeHost(host);
        this.method = normalizeText(method, "method").toUpperCase(Locale.ROOT);
        this.path = normalizePath(path);
        if (issuedAt <= 0) {
            throw new IllegalArgumentException("issuedAt must be positive");
        }
        this.issuedAt = issuedAt;
        this.nonce = normalizeText(nonce, "nonce");
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getHost() {
        return host;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public String getNonce() {
        return nonce;
    }

    private static String normalizeHost(String value) {
        String authority = normalizeText(value, "host");
        try {
            URI uri = URI.create("//" + authority);
            if (uri.getScheme() != null || uri.getRawUserInfo() != null || uri.getHost() == null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("host must be an authority only");
            }
            validatePortSyntax(uri.getRawAuthority(), uri.getPort());
            String normalized = uri.getHost().toLowerCase(Locale.ROOT);
            if (normalized.endsWith(".")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("host must not be blank");
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid host", ex);
        }
    }

    private static void validatePortSyntax(String authority, int port) {
        int closingBracket = authority.lastIndexOf(']');
        int colon = authority.lastIndexOf(':');
        boolean hasPortDelimiter = colon >= 0 && (closingBracket < 0 || colon > closingBracket);
        if (hasPortDelimiter && (port < 0 || port > 65535)) {
            throw new IllegalArgumentException("invalid port");
        }
    }

    private static String normalizePath(String value) {
        String candidate = normalizeText(value, "path");
        try {
            URI uri = URI.create(candidate);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("path must not contain scheme, authority, query, or fragment");
            }
            String rawPath = uri.getRawPath();
            if (rawPath == null || rawPath.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            return removeDotSegments(rawPath.startsWith("/") ? rawPath : "/" + rawPath);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid path", ex);
        }
    }

    private static String removeDotSegments(String rawPath) {
        String[] segments = rawPath.split("/", -1);
        List<String> normalized = new ArrayList<>();
        for (int index = 1; index < segments.length; index++) {
            String segment = segments[index];
            if (".".equals(segment)) {
                if (index == segments.length - 1) {
                    normalized.add("");
                }
                continue;
            }
            if ("..".equals(segment)) {
                if (!normalized.isEmpty()) {
                    normalized.remove(normalized.size() - 1);
                }
                if (index == segments.length - 1) {
                    normalized.add("");
                }
                continue;
            }
            normalized.add(segment);
        }
        return "/" + String.join("/", normalized);
    }

    private static String normalizeText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.indexOf('\r') >= 0 || normalized.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field + " must not contain CR or LF");
        }
        return normalized;
    }
}
