package com.erp.common.mybatis;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Closed-by-default gate for traffic that depends on a validated tenant schema.
 */
public class TenantSchemaReadinessGate {
    private final AtomicBoolean open = new AtomicBoolean(false);

    /**
     * Opens the gate after tenant schema validation succeeds.
     */
    public void open() {
        open.set(true);
    }

    /**
     * Returns whether schema-dependent traffic may proceed.
     *
     * @return true after successful schema validation
     */
    public boolean isOpen() {
        return open.get();
    }
}
