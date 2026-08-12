# Dedicated SaaS deployment

This template runs one tenant on the same ERP application artifacts used by the shared edition. The gateway fails startup unless `ERP_SAAS_BOUND_TENANT_ID` and `ERP_SAAS_BOUND_HOST` are configured, and rejects control-plane resolutions for any other tenant, host, or deployment mode.

## Build the Java images

Build the application jars once, then use the shared Java runtime image definition for every service:

```powershell
mvn -DskipTests package
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-gateway/target/erp-gateway-1.0.0.jar -t registry.example.com/erp/erp-gateway:1.0.0 .
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-auth/target/erp-auth-1.0.0.jar -t registry.example.com/erp/erp-auth:1.0.0 .
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-modules/erp-system/target/erp-system-1.0.0.jar -t registry.example.com/erp/erp-system:1.0.0 .
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-modules/erp-business/target/erp-business-1.0.0.jar -t registry.example.com/erp/erp-business:1.0.0 .
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-modules/erp-workflow/target/erp-workflow-1.0.0.jar -t registry.example.com/erp/erp-workflow:1.0.0 .
docker build -f deploy/saas/Dockerfile.java --build-arg SERVICE_JAR=erp-modules/erp-ai/target/erp-ai-1.0.0.jar -t registry.example.com/erp/erp-ai:1.0.0 .
```

Build the UI image from the `erp-ui` repository with its `deploy/saas/Dockerfile`.

## Deploy

1. Copy `.env.example` to `.env` outside source control and replace all blank secrets.
2. Register the deployment reference and external secret reference in the control plane. Never enter a database password, token, or connection string in that record.
3. For production, point MySQL, Redis, and Nacos variables at managed services and run:

```powershell
docker compose --env-file .env -f docker-compose.dedicated.yml config
docker compose --env-file .env -f docker-compose.dedicated.yml up -d
```

For an isolated evaluation environment only, set the infrastructure hostnames in `.env` to `mysql`, `redis`, and `nacos`, then enable the bundled services:

```powershell
docker compose --env-file .env -f docker-compose.dedicated.yml --profile bundled-infra up -d
```

Place the TLS reverse proxy in front of `127.0.0.1:${ERP_HTTP_PORT}` and preserve the original `Host` header. Verify every `/actuator/health/readiness` check before switching DNS. Database upgrade runners remain enabled and apply the same idempotent scripts as the shared edition.
