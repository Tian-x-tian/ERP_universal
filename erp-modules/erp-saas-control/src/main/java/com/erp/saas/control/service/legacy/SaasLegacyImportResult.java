package com.erp.saas.control.service.legacy;

public record SaasLegacyImportResult(int discovered, int imported, int skipped) {
    public SaasLegacyImportResult {
        if (discovered < 0 || imported < 0 || skipped < 0 || imported + skipped != discovered) {
            throw new IllegalArgumentException("Legacy import counts are inconsistent");
        }
    }
}
