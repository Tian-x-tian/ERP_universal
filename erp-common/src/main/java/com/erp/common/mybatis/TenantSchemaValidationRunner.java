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

    /**
     * Creates the validation runner.
     *
     * @param tenantSchemaValidator tenant schema validator
     */
    public TenantSchemaValidationRunner(TenantSchemaValidator tenantSchemaValidator) {
        this.tenantSchemaValidator = tenantSchemaValidator;
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantSchemaValidator.validate();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
