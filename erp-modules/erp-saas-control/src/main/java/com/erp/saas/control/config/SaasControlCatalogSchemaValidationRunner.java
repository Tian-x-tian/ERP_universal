package com.erp.saas.control.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@ConditionalOnProperty(prefix = "erp.saas.schema-validation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaasControlCatalogSchemaValidationRunner implements ApplicationRunner, Ordered {
    private final SaasControlCatalogSchemaValidator validator;

    @Autowired
    public SaasControlCatalogSchemaValidationRunner(DataSource dataSource) {
        this(new SaasControlCatalogSchemaValidator(dataSource));
    }

    SaasControlCatalogSchemaValidationRunner(SaasControlCatalogSchemaValidator validator) { this.validator = validator; }

    @Override public void run(ApplicationArguments args) { validator.validate(); }
    @Override public int getOrder() { return 200; }
}
