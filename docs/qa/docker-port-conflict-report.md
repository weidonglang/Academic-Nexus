# Docker Port Conflict Report

Updated: 2026-06-28

| Service | Container Port | Default Host Port |
| --- | --- | --- |
| Main | 8080 | 8088 |
| Frontend | 5173 | 5174 |
| MySQL | 3306 | 13306 |
| Redis | 6379 | 16379 |
| Nacos HTTP | 8848 | 18848 |
| Nacos gRPC | 9848 | 19848 |
| AI service | 8090 | 18090 |

Verification:

```powershell
docker compose config
powershell -ExecutionPolicy Bypass -File .\scripts\check-ports.ps1
docker compose up -d
```

All passed on 2026-06-28. Compose also now waits for Nacos health before starting Java services and sets `SPRING_CLOUD_NACOS_DISCOVERY_FAIL_FAST=false` to avoid startup-race exits.
