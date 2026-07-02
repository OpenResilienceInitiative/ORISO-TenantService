# Local Development

This setup runs TenantService locally on `localhost:8081`, lets the frontend on
`localhost:9001` call it directly, and points downstream service calls to the local UserService
or the configured remote dev API.

## 1. Java

Use Java 17:

```bash
/usr/libexec/java_home -V
```

The local run script example auto-detects Java 17 on macOS when `JAVA_HOME` is not already set.

## 2. Create `.env`

Copy the sample config:

```bash
cp .env.example .env
```

Fill all `CHANGE_ME` values.

Important values for the mixed local setup:

```env
SERVER_SERVLET_CONTEXT_PATH=/service
SPRING_LIQUIBASE_ENABLED=false
CONSULTING_TYPE_SERVICE_API_URL=https://api.oriso-dev.site/service
USER_SERVICE_API_URL=http://localhost:8082/service
TENANT_CORS_ENABLED=true
TENANT_CORS_ALLOWED_ORIGINS=http://localhost:9001,http://127.0.0.1:9001
TENANT_CORS_ALLOWED_PATHS=/**
```

`SERVER_SERVLET_CONTEXT_PATH=/service` means callers must use:

```text
http://localhost:8081/service
```

If UserService calls this local TenantService, configure UserService with:

```env
TENANT_SERVICE_API_URL=http://localhost:8081/service
```

## 3. Create `run-local-remote-db.sh`

Copy the example script:

```bash
cp run-local-remote-db.sh.example run-local-remote-db.sh
chmod +x run-local-remote-db.sh
```

`run-local-remote-db.sh` is ignored by git because it may contain local secrets.

## 4. Run

```bash
./run-local-remote-db.sh
```

Expected local base URL:

```text
http://localhost:8081/service
```

## 5. Useful Checks

Check whether TenantService is listening:

```bash
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

Check public tenant lookup with the local context path:

```bash
curl http://localhost:8081/service/tenant/public/id/1
```

Stop the service with `Ctrl+C` in the terminal running the app.

## Frontend Pairing

Use the frontend local setup with:

```env
REACT_APP_TENANT_SERVICE_ORIGIN=http://localhost:8081
REACT_APP_LOCAL_TENANT_ID=1
```

`REACT_APP_LOCAL_TENANT_ID` sets a localhost `tenantId` cookie because localhost has no tenant
subdomain. If the local tenant id variable is removed, the frontend keeps the normal cluster/domain
tenant resolution behavior.
