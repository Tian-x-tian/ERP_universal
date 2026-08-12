package com.erp.saas.control.service.provisioning.model;

import com.erp.saas.contract.model.DeploymentMode;

import java.net.URI;
import java.util.regex.Pattern;

public record SaasTenantProvisioningCommand(
        String requestId,
        String tenantId,
        String slug,
        String tenantName,
        String companyCode,
        String companyName,
        String adminUsername,
        String adminDisplayName,
        String adminEmail,
        DeploymentMode deploymentMode,
        String planCode,
        String host,
        String deploymentRef,
        String secretRef) {
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?");
    private static final Pattern PLAN_CODE = Pattern.compile("[a-z][a-z0-9_.-]{1,63}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public SaasTenantProvisioningCommand {
        requestId = text(requestId, "requestId", 128);
        tenantId = matching(tenantId, "tenantId", TENANT_ID);
        slug = matching(slug == null ? null : slug.toLowerCase(), "slug", SLUG);
        tenantName = text(tenantName, "tenantName", 128);
        companyCode = text(companyCode, "companyCode", 64);
        companyName = text(companyName, "companyName", 128);
        adminUsername = text(adminUsername, "adminUsername", 64);
        adminDisplayName = text(adminDisplayName, "adminDisplayName", 128);
        adminEmail = matching(adminEmail == null ? null : adminEmail.toLowerCase(), "adminEmail", EMAIL);
        if (deploymentMode == null) throw invalid("deploymentMode must not be null");
        planCode = matching(planCode == null ? null : planCode.toLowerCase(), "planCode", PLAN_CODE);
        host = text(host == null ? null : host.toLowerCase(), "host", 253);
        deploymentRef = deploymentRef(deploymentRef, deploymentMode);
        secretRef = secretRef(secretRef, deploymentMode);
    }

    private static String deploymentRef(String value, DeploymentMode mode) {
        String normalized = text(value, "deploymentRef", 255);
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw invalid("deploymentRef must be an HTTP service base URL without credentials or query data");
            }
        } catch (IllegalArgumentException error) {
            if (error instanceof ProvisioningInputException) throw error;
            throw invalid("deploymentRef has an invalid format");
        }
        return normalized;
    }

    private static String secretRef(String value, DeploymentMode mode) {
        if (mode == DeploymentMode.SHARED) {
            if (value != null && !value.isBlank()) throw invalid("secretRef must be empty for shared deployments");
            return null;
        }
        String normalized = text(value, "secretRef", 255);
        if (!normalized.matches("[A-Za-z][A-Za-z0-9+.-]*://[^\\s]+")) {
            throw invalid("secretRef must be an external secret-manager reference");
        }
        return normalized;
    }

    private static String matching(String value, String field, Pattern pattern) {
        String normalized = text(value, field, 512);
        if (!pattern.matcher(normalized).matches()) throw invalid(field + " has an invalid format");
        return normalized;
    }

    private static String text(String value, String field, int maximumLength) {
        if (value == null || value.trim().isEmpty()) throw invalid(field + " must not be blank");
        String normalized = value.trim();
        if (normalized.length() > maximumLength) throw invalid(field + " is too long");
        return normalized;
    }

    private static ProvisioningInputException invalid(String message) {
        return new ProvisioningInputException(message);
    }

    public static final class ProvisioningInputException extends IllegalArgumentException {
        public ProvisioningInputException(String message) {
            super(message);
        }
    }
}
