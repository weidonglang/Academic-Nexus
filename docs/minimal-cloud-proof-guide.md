# EduNexus AI v2.0.2 Minimal Cloud Proof Guide

This guide explains the minimal Spring Cloud proof layer added for teacher verification. The goal is screenshotable, runnable evidence for Gateway, Nacos, Sentinel, Seata and OpenFeign without splitting the existing academic business into risky extra services.

## Scope

Included:

- Nacos service discovery for `academic-main`, `academic-ai-service`, and `edunexus-gateway`.
- Spring Cloud Gateway service on host port `9000`.
- Gateway route `/api/** -> lb://academic-main`.
- Sentinel flow control on the real login endpoint `/api/auth/login`, default QPS `3`.
- OpenFeign proof endpoint calling `academic-ai-service`.
- Seata proof transaction using demo-only tables `cloud_tx_demo_main` and `cloud_tx_demo_ai`.

Not included:

- Splitting courses, grades, exams or applications into new microservices.
- Applying Seata to core academic transactions.
- Replacing existing local IDEA startup on `localhost:8080`.

## Start With Docker Compose

```powershell
copy .env.example .env
docker compose config
docker compose up -d --build
docker compose ps
```

Important host URLs:

| Component | URL |
| --- | --- |
| Gateway | `http://localhost:9000` |
| Main service | `http://localhost:8088` |
| AI service | `http://localhost:18090/internal/ai/status` |
| Nacos | `http://localhost:18848/nacos` |
| Seata console | `http://localhost:7091` |

## Nacos Screenshot

Open:

```text
http://localhost:18848/nacos
```

Expected services:

```text
academic-main
academic-ai-service
edunexus-gateway
```

## Gateway And OpenFeign Proof

```powershell
curl http://localhost:9000/api/cloud-proof/feign/ai-status
```

Expected evidence:

```text
"transport":"OpenFeign"
"targetService":"academic-ai-service"
```

This proves the host calls Gateway `9000`, Gateway routes to `academic-main`, and the main service calls `academic-ai-service` by OpenFeign.

## Sentinel Login Flow Control Proof

PowerShell example:

```powershell
1..8 | ForEach-Object {
  curl -s -X POST http://localhost:9000/api/auth/login `
    -H "Content-Type: application/json" `
    -d '{"username":"demo","password":"wrong"}'
}
```

Expected evidence after the QPS threshold is exceeded:

```text
登录请求过于频繁，请稍后再试
```

The protected resource is the real login method, not a fake demo endpoint. QPS can be adjusted with:

```env
SENTINEL_LOGIN_ENABLED=true
SENTINEL_LOGIN_QPS=3
```

## Seata Distributed Transaction Proof

Commit proof:

```powershell
curl -X POST http://localhost:9000/api/cloud-proof/seata/commit
```

Expected data:

```json
{
  "action": "commit",
  "seataEnabled": true,
  "mainExists": true,
  "aiExists": true
}
```

Rollback proof:

```powershell
curl -X POST http://localhost:9000/api/cloud-proof/seata/rollback
```

Expected data:

```json
{
  "action": "rollback",
  "seataEnabled": true,
  "mainExists": false,
  "aiExists": false
}
```

The transaction writes:

- `academic-main` -> `cloud_tx_demo_main`
- `academic-ai-service` -> `cloud_tx_demo_ai`

The rollback endpoint intentionally throws after both writes. Seata should roll back both tables, and the response keeps the intentional error message so the screenshot can explain why rollback was triggered.

## Local IDEA Mode

For normal local development, keep using:

```text
http://localhost:8080
```

If Seata Server is not running, disable Seata:

```powershell
$env:SEATA_ENABLED="false"
```

Nacos discovery is also optional in local development. If disabled, the main system falls back to direct AI service URL mode where applicable.
