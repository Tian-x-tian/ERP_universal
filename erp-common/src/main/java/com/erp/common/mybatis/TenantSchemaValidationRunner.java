package com.erp.common.mybatis;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * Runs tenant schema validation after schema upgrade runners complete.
 */
public class TenantSchemaValidationRunner implements ApplicationRunner, Ordered {
    private static final int ORDER = 200;

    private final TenantSchemaValidator tenantSchemaValidator;
    private final TenantSchemaReadinessGate readinessGate;

    /**
     * Creates the validation runner.
     *
     * @param tenantSchemaValidator tenant schema validator
     * @param readinessGate tenant schema readiness gate
     */
    public TenantSchemaValidationRunner(TenantSchemaValidator tenantSchemaValidator,
                                        TenantSchemaReadinessGate readinessGate) {
        this.tenantSchemaValidator = tenantSchemaValidator;
        this.readinessGate = readinessGate;
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantSchemaValidator.validate();
        readinessGate.open();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
